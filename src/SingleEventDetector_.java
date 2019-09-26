import ij.*;
import ij.process.*;
import ij.text.TextWindow;
import ij.gui.*;
import ij.plugin.frame.*;
import java.awt.Color;
import java.awt.Frame;
import ij.io.*;
import ij.plugin.*;
import java.awt.image.*;
import java.util.Locale;
import ij.measure.*;

public class SingleEventDetector_  implements PlugIn, Measurements  {

	private int particleDiameter = 3;
	private float pixelSize = 1;
	private int[] particleMask;
	private int boxN = 3;
	private int boxSize = 2*boxN*particleDiameter+1;
	ImagePlus impbFiltered;
	ImagePlus impOriginal;
	ImagePlus impbSelected;
	ImagePlus impInicial;
	private int channel=1;
	protected boolean invert; 
	protected int[] pixels;
	ImageConverter ic;
	ImageConverter ic2;
	ImageConverter ic3;
	ColorModel cm;
	OvalRoi oval2;
	private float meanInside;
	private float meanOutside;
	private float std;
	private float parameter=3;
	private float pooledVar;
	private int points;
	
	public void drawParticleMask() {
		particleMask = new int[boxSize*boxSize];
		int center=boxN*particleDiameter;
		for (int i=0; i<boxSize; i++)
			for (int j=0; j<boxSize; j++)
				particleMask[i*boxSize+j]=((i-center)*(i-center)+(j-center)*(j-center)<=particleDiameter*particleDiameter)? 1:0;
	}
	
	public ImageProcessor cropParticle(ImagePlus impbSelected, float xif, float yif) {
 		int [] dim=impbSelected.getDimensions();
		int xi=Math.round(xif);
		int yi=Math.round(yif);
		int x0=xi-boxN*particleDiameter;//inicial
		int xF=xi+boxN*particleDiameter;//final
		int y0=yi-boxN*particleDiameter;
		int yF=yi+boxN*particleDiameter;
		if (x0>=0 && y0>=0 && xF<dim[0] && yF<dim[1]){
			impbSelected.setRoi(x0,y0,boxSize,boxSize);
			return impbSelected.crop().getProcessor();
		}
		else
			return null;
	}
	
	public boolean evaluateParticle(float xif, float yif)
	{
		ImageProcessor piece=cropParticle(impbFiltered,xif,yif);
		if (piece==null)
			return false;
		float sumOutside=0.0f, sumInside=0.0f, sum2Outside=0.0f, sum2Inside=0.0f, Noutside=0.0f, Ninside=0.0f;
		for (int n=0; n<particleMask.length; n++) {
			float pixelval=piece.getf(n);
			if (particleMask[n]==1) {
				sumInside+=pixelval;
				sum2Inside+=pixelval*pixelval;
				Ninside+=1.0f;
			
			}else{
				sumOutside+=pixelval;
				sum2Outside+=pixelval*pixelval;
				Noutside+=1.0f;
			}
		}
		meanInside=sumInside/Ninside;
		meanOutside=sumOutside/Noutside;
		float varInside=Math.max(0.0f, sum2Inside/Ninside-meanInside*meanInside);//sum2Inside es pixelval*pixelval que es el x2
		float varOutside=Math.max(0.0f, sum2Outside/Noutside-meanOutside*meanOutside);
		pooledVar=((Ninside-1)*varInside+(Noutside-1)*varOutside)/(Ninside+Noutside);
		std=(float)Math.sqrt(pooledVar);
		parameter = (meanInside-meanOutside)/std;
		return parameter>3;
	}
	public void addImage(ImageProcessor ipb1, ImageProcessor ipb2){
		int width = ipb1.getWidth();  
	    int height = ipb1.getHeight();
	    for (int y=0; y<height; y++) {  
	        for (int x=0; x<width; x++) {  
	            ipb1.setf(x,y,ipb1.getf(x,y)+ipb2.getf(x,y));
	        }  
	    }  		
	}

	public void divideByConstant(ImageProcessor ipb, float K){
		int width = ipb.getWidth();  
	    int height = ipb.getHeight();
	    final float iK=1.0f/K;
	    for (int y=0; y<height; y++) {  
	        for (int x=0; x<width; x++) {  
	            ipb.setf(x,y,ipb.getf(x,y)*iK);
	        }  
	    }  		
	}
	public void drawOval(int x, int y, int width, int height) {
		if ((int)particleDiameter*particleDiameter>4*this.particleDiameter*this.particleDiameter) return;
	}
	@SuppressWarnings("deprecation")
	public void run(String arg) {
		 ImagePlus imp = IJ.getImage();
		 impInicial =imp.duplicate();
 		impOriginal=imp.duplicate();
 		int channelToExtract = 1 ;	
		if (impInicial == null)
			 IJ.noImage();
 		if (!showDialog(imp))
 			return;
 		if (impInicial.getNChannels() > 1) 
			channelToExtract=channel;
		ImagePlus impb = extractChannel(impInicial, channelToExtract);
 		IJ.run(impb, "Bandpass Filter...", String.format(Locale.ROOT,
 				"filter_large=%d filter_small=%d suppress=None tolerance=5 autoscale saturate",
 				3*particleDiameter, particleDiameter));
 		impbFiltered=impb.duplicate();
 		impbSelected=impb.duplicate();
 		IJ.run(impb, "Detect Molecules", String.format(Locale.ROOT,
 				"task=[Detect molecules and fit] psf=%d pixel=1 parallel=500 fitting=10 mark ignore",
 				particleDiameter));
 		impb.hide();
 		Frame FR = WindowManager.getFrame("Results");
 		if (FR instanceof TextWindow) 
 			((TextWindow)FR).hide();
 		ResultsTable RT = ResultsTable.getResultsTable(); 		
 		float [] x = RT.getColumn(0);//vector float con valores x de la tabla de resultados
 		float [] y = RT.getColumn(1);//vector float con los valores y de la tabla de resultados
 		boolean [] coordMask = new boolean[x.length];
 		drawParticleMask();
 		int Nok=0;
 		FloatProcessor ipPSF = new FloatProcessor(boxSize, boxSize);
 		for (int i=0; i<x.length; i++) {
         	coordMask[i]=evaluateParticle(x[i],y[i]);
 			if (coordMask[i]==true) {
 				OvalRoi oval = new OvalRoi((int)(x[i]-0.5*boxSize),(int)(y[i]-0.5*boxSize), boxSize, boxSize);	
 				impbSelected.getProcessor().draw(oval);
 				int width = boxSize;
 				impbSelected.getProcessor().setLineWidth(width);
 				impbSelected.getProcessor().setColor(Color.RED);
  				addImage(ipPSF, cropParticle(impbSelected,x[i],y[i]));
 	 			Nok+=1;
 	 			oval2 = new OvalRoi((int)(x[i]-0.5*boxSize),(int)(y[i]-0.5*boxSize),boxSize, boxSize);
 	 		RoiManager manager = RoiManager.getInstance();
 	 		  if (manager == null)
 	 		     {
 	 		        manager = new RoiManager();
 	 		     }
 	 		  manager.addRoi(oval2);
 	 		  manager.runCommand(impOriginal,"Show All with labels");
			}
 		}
 		imp = IJ.getImage();
 		IJ.run("From ROI Manager", "");
 		ImagePlus imp2 = HyperStackConverter.toHyperStack(imp, imp.getNChannels(), imp.getNSlices(),imp.getNFrames(),"Color");
 		impOriginal.hide();
 		impInicial.hide();
 		impb.hide();
 		imp2.show();
 		imp.close();
 		ic3 = new ImageConverter(impbSelected);
 		ic3.convertToGray8();
 		impbSelected.updateAndDraw();
		impbSelected.hide();
		IJ.log(String.format("Number of selected spots: %d",Nok));
 		divideByConstant(ipPSF,Nok);
 		ImagePlus impPSF = new ImagePlus("Recorded Image of Single Particle",ipPSF);
 		ic2 = new ImageConverter(impPSF);
 		ic2.convertToGray8();
 		impPSF.updateAndDraw();
		impPSF.show();
		double [] psfProfile = new double[boxSize];
		double [] xPSF = new double[boxSize];
		double psfMax=-1e38;
        for (int j=0; j<boxSize; j++)
        {
        	psfProfile[j] = ipPSF.getf(boxSize/2,j);
        	xPSF[j] = j-boxSize/2;
        	psfMax=Math.max(psfMax, psfProfile[j]);
        }
        for (int j=0; j<boxSize; j++)
        	psfProfile[j] /=psfMax;
        CurveFitter cf = new CurveFitter(xPSF,psfProfile);
        cf.doFit(CurveFitter.GAUSSIAN);
        double []prms = cf.getParams();
        double std = prms[3];
        IJ.log(String.format("FWHM: %f nm",std*Math.sqrt(8*Math.log(2))*pixelSize));
      	IJ.log(String.format("R2: %f",cf.getFitGoodness()));
      	Plot plot = cf.getPlot(points);
      	plot.setXYLabels("Position (pixels)", "Normalized Intensity");
      	String labels ="GaussianFit\tPSFOriginal\t";
      	plot.setLegend(labels, Plot.AUTO_POSITION);
      	plot.show();
      	Frame FR2 = WindowManager.getFrame(cf.getFormula());
 		if (FR2 instanceof TextWindow) 
 			((TextWindow)FR2).setTitle("PSF Profile");
}
	boolean showDialog(ImagePlus imp) {
		GenericDialog gd = new GenericDialog("PSF Detector");
		gd.addNumericField("Particle diameter", particleDiameter, 0, 3, "pixels");
		gd.addNumericField("Pixel size", 1.0, 0, 3, "nm");
		gd.addNumericField("(meanInside-meanOutside)/std",parameter,0,3, null);
		if (imp.getNChannels() > 1)
    		gd.addStringField("Channel to process ( 1,2,3,...)", "1");
		gd.showDialog();
		if(gd.wasCanceled())
			return false;
		if(gd.invalidNumber()) {
			IJ.error("Error", "Invalid input number");
			return false;
		}				
		particleDiameter = (int)(gd.getNextNumber());
		pixelSize = (float)(gd.getNextNumber());
		parameter = (float)(gd.getNextNumber());
		if (imp.getNChannels() > 1){
			String channelStr= gd.getNextString();
			if (channelStr  =="R")
				channel = 1;
			else if (channelStr=="G")
				channel = 2;
			else if (channelStr =="B")
				channel = 3;
			else
				channel=Integer.parseInt(channelStr);
		}
		return true;
	}
	private ImagePlus extractChannel(ImagePlus imp, int channel) {
        int width = imp.getWidth();
        int height = imp.getHeight();
        int zslices = imp.getNSlices();
        int frames = imp.getNFrames();
        FileInfo fileInfo = imp.getOriginalFileInfo();
        ImageStack stack2 = new ImageStack(width, height);
        ColorProcessor cp = new ColorProcessor(imp.getImage());
        ByteProcessor  bp = cp.getChannel(channel, null);
        ImagePlus imp2 = new ImagePlus("C" + channel + "-" + imp.getTitle(), bp);
        for (int t = 1; t <= frames; t++)
            for (int z = 1; z <= zslices; z++) {
                int slice = imp.getStackIndex(channel, z, t);
                stack2.addSlice("", imp.getStack().getProcessor(slice));
            }
        ColorModel cm = LookUpTable.createGrayscaleColorModel(false);
        stack2.setColorModel(cm);
        imp2.setStack(stack2);
        imp2.setDimensions(1, zslices, frames);
        if (zslices * frames > 1)
            imp2.setOpenAsHyperStack(true);
        imp2.setFileInfo(fileInfo);
        imp2.updateAndDraw();
        return imp2;
    }
}

	    
	

	
