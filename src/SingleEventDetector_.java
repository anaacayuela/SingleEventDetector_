import java.awt.Color;
import java.awt.Frame;
import java.util.*;
import java.awt.image.*;
import ij.*;
import ij.process.*;
import ij.text.TextWindow;
import ij.gui.*;
import ij.plugin.frame.*;
import ij.io.*;
import ij.plugin.*;
import ij.measure.*;

public class SingleEventDetector_  implements PlugIn, Measurements  {

	private static final float yif = 0;
	private static final float xif = 0;
	private int particleDiameter = 3;
	private int pixelSize = 1;
	private int slice=1;
	private int frame=1;
	private int[] particleMask;
	private int boxN = 3;
	private int boxSize;
	ImagePlus impbFiltered;
	ImagePlus impOriginal;
	ImagePlus impbSelected;
	ImagePlus impInicial;
	ImagePlus impbShow;
	ImagePlus impb;
	ImagePlus impbShow2;
	ImagePlus impbShow3;
	ImagePlus impbShow4;
	ImagePlus impbShow5;
	private int channel=1;
	protected boolean invert; 
	private static boolean doScaling = true;
	protected int[] pixels;
	ImageConverter ic;
	ImageConverter ic2;
	ImageConverter ic3;
	ImageConverter ic4;
	ColorModel cm;
	OvalRoi oval2;
	private float meanInside;
	private float meanOutside;
	private float std;
	private float top=10;
	private float pooledVar;
	private int points;
	private boolean bPass;
	
	public void drawParticleMask() {
		int R2=particleDiameter*particleDiameter/4;
		particleMask = new int[boxSize*boxSize];
		int center=boxSize/2;
		for (int i=0; i<boxSize; i++)
			for (int j=0; j<boxSize; j++)
				particleMask[i*boxSize+j]=((i-center)*(i-center)+(j-center)*(j-center)<=R2)? 1:0;
	}
	
	public ImageProcessor cropParticle(ImagePlus impbSelected, float xif, float yif)
	{
 		int [] dim=impbSelected.getDimensions();
		int xi=Math.round(xif);
		int yi=Math.round(yif);
		int x0=xi-boxSize/2;
		int xF=xi+boxSize/2;
		int y0=yi-boxSize/2;
		int yF=yi+boxSize/2;
		if (x0>=0 && y0>=0 && xF<dim[0] && yF<dim[1])
		{
			impbSelected.setRoi(x0,y0,boxSize,boxSize);
			return impbSelected.crop().getProcessor();
		}
		else
			return null;
	}
	
	public double evaluateParticle(float xif, float yif)
	{
		ImageProcessor piece=cropParticle(impbFiltered,xif,yif);
		if (piece== null)
			return -1000;
			
		float sumOutside=0.0f, sumInside=0.0f, sum2Outside=0.0f, sum2Inside=0.0f, Noutside=0.0f, Ninside=0.0f;
		for (int n=0; n<particleMask.length; n++)
		{
			float pixelval=piece.getf(n);
			if (particleMask[n]==1)
			{
				sumInside+=pixelval;
				sum2Inside+=pixelval*pixelval;
				Ninside+=1.0f;
			}
			else
			{
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
		return (meanInside-meanOutside)/std;
	}

	public void addImage(ImageProcessor ipb1, ImageProcessor ipb2)
	{
		int width = ipb1.getWidth();  
	    int height = ipb1.getHeight();
	    for (int y=0; y<height; y++) {  
	        for (int x=0; x<width; x++) {  
	            ipb1.setf(x,y,ipb1.getf(x,y)+ipb2.getf(x,y));
	        }  
	    }  		
	}

	public void divideByConstant(ImageProcessor ipb, float K)
	{
		int width = ipb.getWidth();  
	    int height = ipb.getHeight();
	    final float iK=1.0f/K;
	    for (int y=0; y<height; y++) {  
	        for (int x=0; x<width; x++) {  
	            ipb.setf(x,y,ipb.getf(x,y)*iK);
	        }  
	    }  		
	}
	@SuppressWarnings("deprecation")
	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		impInicial =imp.duplicate();
 		impOriginal=imp.duplicate();
 		int channelToExtract = 1;
 		int sliceToExtract=1;
 		int frameToExtract=1;
		if (impInicial == null)
			 IJ.noImage();
 		if (!showDialog(imp))
 			return;
 		if (impInicial.getStackSize() == 1) {
 			impb = impInicial;
 		if (impInicial.getType() == ImagePlus.COLOR_RGB) {
 	 		IJ.run(impInicial, "8-bit", "");
 	 		IJ.run(impInicial, "Grays", "");
 	 		impb=impInicial;
 		}
 	}
 		if (impInicial.getNChannels() > 1) { 
		   channelToExtract=channel;
		   impb = extractChannel(impInicial,channelToExtract);
 		}
 		if (impInicial.getNSlices() > 1) {
 		   sliceToExtract=slice;
 		   impb=extractZSlice(impInicial, sliceToExtract);
 		   impbShow = impb.duplicate();
 		}
 		if (impInicial.getNChannels() == 1 &&impInicial.getNFrames() > 1) {
 		   frameToExtract=frame;
 		   impb = extractTFrame(impInicial, frameToExtract);
 		   impbShow3 = impb.duplicate();
 		}
		if (impInicial.getNChannels() > 1 && impInicial.getNSlices() > 1 && impInicial.getStackSize() > 1) {
		   sliceToExtract=slice;
		   channelToExtract=channel;
		   impb =extractSliceChannel(impInicial, sliceToExtract, channelToExtract);
		   impbShow5= impb.duplicate();
		}
		if (impInicial.getNChannels() > 1 && impInicial.getNFrames() > 1 && impInicial.getStackSize() > 1) {
			   frameToExtract=frame;
			   channelToExtract=channel;
			   impb =extractFrameChannel(impInicial, sliceToExtract, channelToExtract);
			   impbShow4=impb.duplicate();
			}
		if (impInicial.getNChannels() > 1 && impInicial.getNSlices() > 1  && impInicial.getNFrames() >  1 && impInicial.getStackSize() > 1){
			sliceToExtract=slice;
			channelToExtract=channel;
			frameToExtract=frame;
			impb =extractSliceChannelFrame(impInicial, sliceToExtract, channelToExtract,frameToExtract);
			impbShow2 = impb.duplicate();
		}
		double stdParticle=particleDiameter/6.0;
		boxSize = (int)Math.ceil(boxN*particleDiameter/2+1);
		impbFiltered = impb.duplicate();
 		impbSelected=impb.duplicate();
 		if (bPass == true ) {
 		IJ.run(impb, "Bandpass Filter...",  String.format(Locale.ROOT,"filter_large=%d filter_small=%d suppress=None tolerance=5 autoscale saturate",particleDiameter,particleDiameter/3));
 		}
 		IJ.run(impb, "Detect Molecules", String.format(Locale.ROOT,
 				"task=[Detect molecules and fit] psf=%f pixel=%d parallel=500 fitting=10 mark ignore",
 				stdParticle,pixelSize));
 		impb.hide();
 		ResultsTable RT = ResultsTable.getResultsTable(); 		
 		float [] x = RT.getColumn(0);
 		float [] y = RT.getColumn(1);
 		if (x==null)
 			return;
 		double [] coordZscore = new double[x.length];
 		drawParticleMask();
 		int Nok=0;
 		FloatProcessor ipPSF = new FloatProcessor(boxSize, boxSize);
 		for (int i=0; i<x.length; i++)
         	coordZscore[i]=evaluateParticle(x[i],y[i]);
 		double [] coordZscoreSorted = new double[x.length];
 		System.arraycopy(coordZscore, 0, coordZscoreSorted, 0, coordZscore.length);
 		Arrays.sort(coordZscoreSorted);
 		double threshold=coordZscoreSorted[(int)((1-Math.max(Math.min(top,100),0)/100.0)*x.length)];
   		for (int i=0; i<x.length; i++)
     	{
 			 if(coordZscore[i]>threshold)
 			 {
 				OvalRoi oval = new OvalRoi((int)(x[i]-0.5*particleDiameter), (int)(y[i]-0.5*particleDiameter),
 						particleDiameter, particleDiameter);	
 				impbSelected.getProcessor().draw(oval);
 				int width =boxSize;
 				impbSelected.getProcessor().setLineWidth(width);
 				impbSelected.getProcessor().setColor(Color.RED);
  				addImage(ipPSF, cropParticle(impbSelected,x[i],y[i]));
 	 			Nok+=1;
 	 		    RoiManager manager = RoiManager.getInstance();
 	 		    if (manager == null)
 	 		        manager = new RoiManager();
                manager.addRoi(oval);
			}
 		}
   		if (imp.getNChannels() > 1) {
   			RoiManager rmOne = RoiManager.getInstance();
 	 		imp = IJ.getImage();
 	 		IJ.run("From ROI Manager", "");
 	 		ImagePlus imp2 = HyperStackConverter.toHyperStack(imp, imp.getNChannels(), imp.getNSlices(),imp.getNFrames(),"Color");
 	 		impOriginal.hide();
 	 		impInicial.hide();
 	 		imp2.show();
 	 		rmOne.runCommand(imp2,"Show All with labels");
 	 		imp.close();
 	 		}
 		
 		if (imp.getStackSize() == 1) { 
 			RoiManager rmTwo = RoiManager.getInstance();
 			imp = IJ.getImage();
 	 		IJ.run("From ROI Manager", "");
 	 		impOriginal.hide();
 	 		impInicial.hide();
 	 		imp.show();
 	 		rmTwo.runCommand(imp,"Show All with labels");
 		}
 		 if (imp.getNSlices() > 1) {
 			RoiManager rmThree = RoiManager.getInstance();
 	 		impbShow.show();
 	 		IJ.run("From ROI Manager", "");
 	 		impOriginal.hide();
  			impInicial.hide();
  			rmThree.runCommand(impbShow,"Show All with labels");
  			imp.close();
  		 }
 		if (impInicial.getNChannels() == 1 && imp.getNFrames() > 1) {
 			RoiManager rmSix = RoiManager.getInstance();
 	 		impbShow3.show();
 	 		IJ.run("From ROI Manager", "");
 	 		impOriginal.hide();
  			impInicial.hide();
  			rmSix.runCommand(impbShow3,"Show All with labels");
  			imp.close();
  		 }
 		 
 		if(imp.getNChannels() > 1 && imp.getNSlices() > 1 &&  imp.getStackSize() > 1) {
 			RoiManager rmFour = RoiManager.getInstance();
 	 		imp = IJ.getImage();
 	 		IJ.run("From ROI Manager", "");
 	 		ImagePlus imp2 = HyperStackConverter.toHyperStack(imp, imp.getNChannels(), imp.getNSlices(),imp.getNFrames(),"Color");
 	 		impOriginal.hide();
 	 		impInicial.hide();
 	 		imp2.show();
 	 		rmFour.runCommand(imp2,"Show All with labels");
 	 		imp.close();
 	 		imp.close();
 		}
 		if(imp.getNChannels() > 1 && imp.getNSlices() > 1 && imp.getNFrames() > 1 && imp.getStackSize() > 1) {
 			RoiManager rmFive = RoiManager.getInstance();
 			imp = IJ.getImage();
 			impbShow2.show();
 	 		IJ.run("From ROI Manager", "");
 	 		impOriginal.hide();
 	 		impInicial.hide();
 	 		rmFive.runCommand(impbShow2,"Show All with labels");
 	 		imp.close();
 		}
 		if(imp.getNChannels() > 1 && imp.getNFrames() > 1 && imp.getStackSize() > 1) {
 			RoiManager rmSix = RoiManager.getInstance();
 	 		imp = IJ.getImage();
 	 		IJ.run("From ROI Manager", "");
 	 		ImagePlus imp2 = HyperStackConverter.toHyperStack(imp, imp.getNChannels(), imp.getNSlices(),imp.getNFrames(),"Color");
 	 		impOriginal.hide();
 	 		impInicial.hide();
 	 		imp2.show();
 	 		rmSix.runCommand(imp2,"Show All with labels");
 	 		imp.close();
 	 		imp.close();
 	 		
 		}
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
 		Frame FR = WindowManager.getFrame("Results");
 		if (FR instanceof TextWindow) 
 			((TextWindow)FR).close(false);
}
	boolean showDialog(ImagePlus imp) {
		GenericDialog gd = new GenericDialog("Single Event Detector");
		gd.addNumericField("Pixel size", pixelSize, 0, 3, "nm");
		gd.addNumericField("Particle diameter", particleDiameter, 0, 3, "pixels");
		gd.addNumericField("Percentage of points to keep",top,0,3, null);
		if (imp.getNChannels() > 1) {
    		gd.addStringField("Channel to process ( 1,2,3,...)", "1");
		}
		if (imp.getNSlices() > 1) {
			gd.addNumericField("Slice to process",slice,0,3, "slice");
		}
		if (imp.getNFrames() > 1) {
			gd.addNumericField("Frame to process",frame,0,3, "frame");
		}
		gd.addCheckbox("Apply BandPass Filter (IF THE IMAGE IS TOO NOISY!!!!!)", false);
		gd.showDialog();
		if(gd.wasCanceled())
			return false;
		if(gd.invalidNumber()) {
			IJ.error("Error", "Invalid input number");
			return false;
		}				
		pixelSize = (int)(gd.getNextNumber());
		particleDiameter = (int)(gd.getNextNumber());
		top = (float)(gd.getNextNumber());
		if (imp.getNChannels() > 1) {
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
		if (impInicial.getNSlices() > 1)
			slice=(int)(gd.getNextNumber());	
		if(imp.getNFrames() > 1)
			frame =(int)(gd.getNextNumber());
		bPass = (boolean) gd.getNextBoolean();
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
               			 int sliceOne = imp.getStackIndex(channel, z, t);
               			 stack2.addSlice("", imp.getStack().getProcessor(sliceOne));
            }
        	imp2.setStack(stack2);
        	imp2.setDimensions(1, zslices, frames);
       		if (zslices * frames > 1)
            		imp2.setOpenAsHyperStack(true);
       		imp2.setFileInfo(fileInfo);
       		imp2.updateAndDraw();
        	return imp2;
	}
	private ImagePlus extractZSlice(ImagePlus imp, int slice) {
		  int width    = imp.getWidth();
		  int height   = imp.getHeight();
		  int channels = imp.getNChannels();
		  int frames   = imp.getNFrames();
		  FileInfo fileInfo = imp.getOriginalFileInfo();
		  ImageStack stack2 = new ImageStack(width, height);
		  ImagePlus imp2 = new ImagePlus();
		  imp2.setTitle("Z" + slice + "-" + imp.getTitle());
		  for (int f = 1; f <= frames; f++)
		   	 for (int c = 1; c <= channels; c++) {
		      		int sliceTwo = imp.getStackIndex(c, slice, f);
		      		stack2.addSlice("", imp.getStack().getProcessor(sliceTwo));
		    }
		  imp2.setStack(stack2);
		  imp2.setDimensions(channels, 1, frames);
		  if (channels*frames > 1)
		   	 imp2.setOpenAsHyperStack(true);
		  imp2.setFileInfo(fileInfo);
		  imp2.updateAndDraw();
		  return imp2;
		}
	private ImagePlus extractTFrame(ImagePlus imp, int frame) {
		  int width    = imp.getWidth();
		  int height   = imp.getHeight();
		  int channels = imp.getNChannels();
		  int frames   = imp.getNFrames();
		  int zslices = imp.getNSlices();
		  FileInfo fileInfo = imp.getOriginalFileInfo();
		  ImageStack stack2 = new ImageStack(width, height);
		  ImagePlus imp2 = new ImagePlus();
		  imp2.setTitle("T" + frame + "-" + imp.getTitle());
		  for (int z = 1; z <= zslices; z++)
			  for (int c = 1; c <= channels; c++){
              			int sliceSix = imp.getStackIndex(c, z, frame);
             			stack2.addSlice("", imp.getStack().getProcessor(sliceSix));
          }
		  imp2.setStack(stack2);
		  imp2.setDimensions(channels,zslices ,1 );
		  if (channels*zslices > 1)
		   	 imp2.setOpenAsHyperStack(true);
		  imp2.setFileInfo(fileInfo);
		  imp2.updateAndDraw();
		  return imp2;
		}
	private ImagePlus extractSliceChannel(ImagePlus imp, int slice, int channel) {
		  int width    = imp.getWidth();
		  int height   = imp.getHeight();
		  int channels = imp.getNChannels();
		  int frames   = imp.getNFrames();
		  int zslices = imp.getNSlices();
		  FileInfo fileInfo = imp.getOriginalFileInfo();
		  ImageStack stack2 = new ImageStack(width, height);
		  ColorProcessor cp = new ColorProcessor(imp.getImage());
	     	  ByteProcessor  bp = cp.getChannel(channel, null);
		  ImagePlus imp2 = new ImagePlus("C" + channel + "-" +"Z" + slice + "-"+ imp.getTitle(), bp);
		  for (int f = 1; f <= frames; f++) {
		         int sliceThree = imp.getStackIndex(channel, slice, f);
		     	 stack2.addSlice("", imp.getStack().getProcessor(sliceThree));
		  }
	     
		  imp2.setStack(stack2);
		  imp2.setDimensions(1, 1, frames);
		  if (frames > 1)
		   	 imp2.setOpenAsHyperStack(true);
		  imp2.updateAndDraw();
		  imp2.setFileInfo(fileInfo);
		  return imp2;
	}
	private ImagePlus extractSliceChannelFrame(ImagePlus imp, int slice, int channel, int frame) {
		  int width    = imp.getWidth();
		  int height   = imp.getHeight();
		  int channels = imp.getNChannels();
		  int frames   = imp.getNFrames();
		  int zslices = imp.getNSlices();
		  FileInfo fileInfo = imp.getOriginalFileInfo();
		  ImageStack stack2 = new ImageStack(width, height);
		  ColorProcessor cp = new ColorProcessor(imp.getImage());
	      	  ByteProcessor  bp = cp.getChannel(channel, null);
		  ImagePlus imp2 = new ImagePlus("C" + channel + "-" +"Z" + slice + "-"+"T" + frame + "-"+ imp.getTitle(), bp);
		  int sliceFour = imp.getStackIndex(channel, slice, frame);
	     	  stack2.addSlice("", imp.getStack().getProcessor(sliceFour));
		  imp2.setStack(stack2);
		  imp2.setDimensions(1, 1, 1);
		  imp2.setOpenAsHyperStack(true);
		  imp2.setFileInfo(fileInfo);
		  return imp2;
	}
	private ImagePlus extractFrameChannel(ImagePlus imp, int frame, int channel) {
		  int width    = imp.getWidth();
		  int height   = imp.getHeight();
		  int channels = imp.getNChannels();
		  int frames   = imp.getNFrames();
		  int zslices = imp.getNSlices();
		  FileInfo fileInfo = imp.getOriginalFileInfo();
		  ImageStack stack2 = new ImageStack(width, height);
		  ColorProcessor cp = new ColorProcessor(imp.getImage());
	     	  ByteProcessor  bp = cp.getChannel(channel, null);
		  ImagePlus imp2 = new ImagePlus("C" + channel + "-" +"T" + frame + "-"+ imp.getTitle(), bp);
		  for (int z = 1; z <= zslices; z++) {
		     	 	int sliceThree = imp.getStackIndex(channel, z, frame);
		      		stack2.addSlice("", imp.getStack().getProcessor(sliceThree));
		  }
		  imp2.setStack(stack2);
		  imp2.setDimensions(1, zslices, 1);
		  if (zslices > 1)
			    imp2.setOpenAsHyperStack(true);
	          imp2.updateAndDraw();
		  imp2.setFileInfo(fileInfo);
		  return imp2;
	}
}
