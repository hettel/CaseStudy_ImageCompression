# ImageCompression-CaseStudy

This project contains a sample application demonstrating the compression of images by applying a two-dimensional Fast Fourier Transformation.

This project is part of a lesson hold on the university of applied sciences Kaiserslautern.

Remarks
* The project was build with OpenJDK 11 
* The application uses [oshi](https://github.com/oshi/oshi) (Native Operating System and Hardware Information)
* Dependencies to JavaFX and the oshi libraries are managed by Maven

Main class: `app.SimpleImageCompression`

Build command:

`mvn clean package`

The command create a lib and module directory that contains the dependencies and module runnable.

Start the application:

`java -p modules;lib -m FFTImageCompression/app.SimpleImageCompression`

---

### The Application

The application offers a simple user interface. You can load a directory with jpg- and png-files. The contents of the directory are shown below on the UI.

<img src="images/AppUI.jpg" alt="drawing" width="500"/>

The selected image will be transformed to a gray image (upper image on the left column). From the gray image a two-dimensional Fourier Transformation is calculated an the absolute values of the Fourier coefficients are shown in lower image on the left column. The biggest coefficient is colored white, zero valued coefficients are colored black. Colors between are scaled log-gray.

On the right side a compression rate can be selected. Pressing the start button the selected 
percent smallest coefficients of the two-dimensional Fourier Transformation are set to zero. With this truncated image information the image is reconstructed. The reconstructed image an the truncated Fourier matrix are both shown on the left column.

**Remark:** The images should be not too big. A size of max 4096 x 4096 pixels is strongly recommended! 

