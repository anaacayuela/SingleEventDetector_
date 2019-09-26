# SingleEventDetector_

  This ImageJ/Fiji plugin was developed to provide a fast-exectuion software and intuitive GUI for quantifying and showing Point-Spread Function (PSF) through the image of a single particle. This plugin requires only few input parametres to determine the following measurements which are very easy to acquire for microscopy users: "Particle Diameter" in pixel units, "Pixel size" in nanometers (nm), "(meanInside-meanOutside)/std" >3 or "Channel to process (1,2,3...)". Parameters as "Pixel size" can be found in the metadata file of the image that is going to be processed. Apart from that, this plugin involve particle location and tracking algorithm that transforms intensities into particle positions, as well as FFT BandPass Filter which computes the inverse Fourier transform to get an accurate recorded sample of a single particle for PSF measurement. Because of this plugin is focus on image processing for life scientists, when talking about the PSF, it is refered to the full width at half maximum or FWHM (nm) which is used as a scale of the width of the PSF. In this plugin, we use the 2D Gaussian function to approximate and model the PSF of based images and then std/σ (Standard Desviation) is considered as the only important parameter to determine a good profile so that we use the relation between the FHM and σ in the Gaussian function.
  
To achive a correct installation and launching, you should download the one single JAR file [SingleEventDetector.jar](https://github.com/anaacayuela/SingleEventDetector_/releases/download/1.0/SingleEventDetector_.jar) and place it into the "plugins" folder of ImageJ/Fiji. Note that for a better understanding of this plugin, you shall do the following steps:

## •STEP 1.
Open the image file you want to analyze through ImageJ/Fiji -> Open... .

![psf1](https://user-images.githubusercontent.com/54528366/65421950-63ad1100-de05-11e9-91a3-9d07f392fb62.png)

## •STEP 2.
Select PSFDetector (ImageJ/Fiji ->Plugings -> PSFDetector).

![Imagen1](https://user-images.githubusercontent.com/54528366/65697710-a8da7880-e07b-11e9-8b0f-27b82616f5d9.png)

## •STEP 3. 
Fill the parameter gaps that appear in the GUI with your own data and try to establish a "(meanInside-meanOutside)/std" >3 for getting an accurate measure and press on "OK" button. 

![Imagen2](https://user-images.githubusercontent.com/54528366/65697916-053d9800-e07c-11e9-91fb-749c3abd98af.png)


## •STEP 4.
At this point, you will obtain a total of 3 images: 

            -The original input image with an overlay of ROIs (Region of Interest) labeled and displayed which are resulted from the                  number of selected particle spots for each slice, channel or frame.
            
 ![Imagen3](https://user-images.githubusercontent.com/54528366/65699095-d88a8000-e07d-11e9-9c22-7406978c0674.png)

 
            -8-bit grayscale image with the recorded single particle spot.
            
![Imagen4](https://user-images.githubusercontent.com/54528366/65699225-0ff92c80-e07e-11e9-897c-68e199d85793.png)


## •STEP 5.
At this point, you'll see an IJ.log with the Detection parameters:

            -Image title.
            -Image pixel size in nm units.
            -Detection SD of PSF in pixel/nm units.
            -Kernel size: pixels
            -Detection time in seconds.
            -Fitting time in seconds.
            -Total time in seconds.
            -Total number of localizations.
            -Number of selected spots.
            -FWHM in nm units.
            -R2: coefficient of determination.
            
![psf8](https://user-images.githubusercontent.com/54528366/65425767-0cac3980-de0f-11e9-97bf-b4dc8a69f8a6.png)


## STEP 6. 
In this point, you will be able to use the ROI (Region of Interest) Manager tool for working with the multiple selected particle spots. 

![Imagen5](https://user-images.githubusercontent.com/54528366/65699905-1340e800-e07f-11e9-8560-027e17e761c6.png)

## STEP 7.
Finally, in this step the ImageJ/Fiji plugin will generate a 2D Point Spread Function Intensity Profile which represent the normalized intensity profile along the postion in pixels through the center of the selected particle spot using the Gaussian curve fitting procedure. Besides, you will obtain fitting paramaters.

![fiittig](https://user-images.githubusercontent.com/54528366/65496511-e5aa4200-deb8-11e9-9381-fe07fc6d1e79.png)

