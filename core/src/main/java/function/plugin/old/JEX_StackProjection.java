package function.plugin.old;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.plugin.ZProjector;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */
public class JEX_StackProjection extends JEXCrunchable {

	public static final String METHOD_MEAN = "mean", METHOD_MIN = "min", METHOD_MAX = "max", METHOD_MEDIAN = "median", METHOD_SUM = "sum", METHOD_STDEV = "std. dev.", METHOD_DIFF = "diff (final-initial)", METHOD_DIFF2 = "diff (initial-final)", METHOD_ABSDIFF = "absolute diff [abs(final-initial)]", METHOD_MULT = "multiply";

	public JEX_StackProjection()
	{}

	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------

	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	@Override
	public String getName()
	{
		String result = "Stack Projection";
		return result;
	}

	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo()
	{
		String result = "Function that allows you to perform math operation along a single dimension (e.g. Z, T) leaving the others unchanged.";
		return result;
	}

	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox()
	{
		String toolbox = "Stack processing";
		return toolbox;
	}

	/**
	 * This method defines if the function appears in the list in JEX It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	@Override
	public boolean showInList()
	{
		return true;
	}

	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public boolean allowMultithreading()
	{
		return true;
	}

	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------

	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	@Override
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(IMAGE, "Image");
		return inputNames;
	}

	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(IMAGE, "Projected Image");

		if(outputNames == null)
			return defaultOutputNames;
		return outputNames;
	}

	/**
	 * Returns a list of parameters necessary for this function to run... Every parameter is defined as a line in a form that provides the ability to set how it will be displayed to the user and what options are available to choose from The simplest
	 * FormLine can be written as: FormLine p = new FormLine(parameterName); This will provide a text field for the user to input the value of the parameter named parameterName More complex displaying options can be set by consulting the FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	@Override
	public ParameterSet requiredParameters()
	{
		Parameter p0 = new Parameter("Dimension", "Name of dimension to perform the operation (case and whitespace sensitive).", "Z");
		Parameter p1 = new Parameter("Math Operation", "Type of math operation to perform.", Parameter.DROPDOWN, new String[] { METHOD_MEAN, METHOD_MAX, METHOD_MIN, METHOD_SUM, METHOD_STDEV, METHOD_MEDIAN, METHOD_DIFF, METHOD_DIFF2, METHOD_ABSDIFF, METHOD_MULT}, 5);
		Parameter p2 = new Parameter("Sliding Window Projection", "Perform the projection for the whole stack or for N number of images at a time, shifting by 1 each time.", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p3 = new Parameter("N", "Number of images in sliding window (ignored if not sliding window).", "2");
		Parameter p4 = getNumThreadsParameter(10, 6);
		Parameter p5 = new Parameter("Exclusion Filter DimTable", "Exclude combinatoins of Dimension Names and values. (Use following notation '<DimName1>=<a1,a2,...>;<DimName2>=<b1,b2,...>' e.g., 'Channel=0,100,100; Time=1,2,3,4,5' (spaces are ok).", Parameter.TEXTFIELD, "");

		// Parameter("New Max","Image Intensity Value","65535.0");
		// Parameter p5 = new
		// Parameter("Gamma","0.1-5.0, value of 1 results in no change","1.0");
		// Parameter p6 = new
		// Parameter("Output Bit Depth","Depth of the outputted image",FormLine.DROPDOWN,new
		// String[] {"8","16","32"},1);

		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p5);
		return parameterArray;
	}

	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------

	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-ridden by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return true;
	}

	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData imageData = inputs.get("Image");
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		DimTable originalDimTable = imageData.getDimTable().copy();

		// Collect Parameters
		String dimName = parameters.getValueOfParameter("Dimension");
		String mathOperation = parameters.getValueOfParameter("Math Operation");
		boolean slidingWindow = Boolean.parseBoolean(parameters.getValueOfParameter("Sliding Window Projection"));
		int slidingWindowSize = Integer.parseInt(parameters.getValueOfParameter("N"));
		String exclusionFilterString = parameters.getValueOfParameter("Exclusion Filter DimTable");
		DimTable filterTable = new DimTable(exclusionFilterString);

		// Run the function
		Dim dimToProject = originalDimTable.getDimWithName(dimName);
		if(dimToProject == null)
		{
			JEXDialog.messageDialog("Couldn't find the specified projection dimension in the image object. Aborting.", this);
			return false;
		}

		DimTable subDimTable = originalDimTable.copy();
		int originalDimIndex = subDimTable.indexOfDimWithName(dimToProject.name());
		subDimTable.remove(originalDimIndex);

		List<DimensionMap> maps = subDimTable.getDimensionMaps();
		TreeMap<DimensionMap,String> dataMap = new TreeMap<DimensionMap,String>();
		String actualPath;
		int count = 0;
		for (DimensionMap map : maps)
		{
			if(slidingWindow)
			{
				for (int i = 0; i <= dimToProject.size() - slidingWindowSize; i++)
				{
					if(this.isCanceled())
					{
						return false;
					}
					
					List<DimensionMap> stackMaps = this.getSomeStackMaps(map, dimToProject, slidingWindowSize, i, filterTable);
					if(stackMaps.size() == 0)
					{
						continue;
					}

					ImageProcessor finalImp = null;
					if(mathOperation.equals(METHOD_DIFF))
					{
						finalImp = evaluate(stackMaps, imageData, false, true);
					}
					else if(mathOperation.equals(METHOD_DIFF2))
					{
						finalImp = evaluate(stackMaps, imageData, false, false);
					}
					else if(mathOperation.equals(METHOD_ABSDIFF))
					{
						finalImp = evaluate(stackMaps, imageData, true, true);
					}
					else if(mathOperation.equals(METHOD_MULT))
					{
						finalImp = multiplicationProjection(stackMaps, imageData);
					}
					else
					{
						finalImp = evaluate(stackMaps, imageData, mathOperation);
					}
					actualPath = JEXWriter.saveImage(finalImp);
					if(actualPath != null)
					{
						DimensionMap saveDim = this.getAStackMap(map, dimToProject, i);
						dataMap.put(saveDim, actualPath);
					}
					// Status bar
					count = count + 1;
					int percentage = (int) (100 * ((double) count / (((double) maps.size()) * (double) (dimToProject.size() - slidingWindowSize + 1))));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
			}
			else
			{
				if(this.isCanceled())
				{
					return false;
				}
				List<DimensionMap> stackMaps = this.getAllStackMaps(map, dimToProject, filterTable);

				ImageProcessor finalImp = null;
				if(mathOperation.equals(METHOD_DIFF))
				{
					finalImp = evaluate(stackMaps, imageData, false, true);
				}
				else if(mathOperation.equals(METHOD_DIFF2))
				{
					finalImp = evaluate(stackMaps, imageData, false, false);
				}
				else if(mathOperation.equals(METHOD_ABSDIFF))
				{
					finalImp = evaluate(stackMaps, imageData, true, true);
				}
				else if(mathOperation.equals(METHOD_MULT))
				{
					finalImp = multiplicationProjection(stackMaps, imageData);
				}
				else
				{
					finalImp = evaluate(stackMaps, imageData, mathOperation);
				}
				actualPath = JEXWriter.saveImage(finalImp);
				if(actualPath != null)
				{
					DimensionMap saveDim = this.getAStackMap(map, dimToProject, 0);
					dataMap.put(saveDim, actualPath);
				}
				// Status bar
				count = count + 1;
				int percentage = (int) (100 * ((double) count / (double) maps.size()));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}

		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), dataMap);
		Dim newDim = null;
		if(slidingWindow)
		{
			newDim = new Dim(dimToProject.name(), dimToProject.valuesUpThrough(dimToProject.size() - slidingWindowSize));
		}
		else
		{
			newDim = new Dim(dimToProject.name(), new String[] { dimToProject.valueAt(0) });
		}
		subDimTable.add(originalDimIndex, newDim);
		// Remove dim values that are filtered out.
		for(Dim d : filterTable)
		{
			Dim d2 = subDimTable.getDimWithName(d.dimName);
			if(d2 != null)
			{
				for(String v : d.dimValues)
				{
					d2.dimValues.remove(d2.index(v));
				}
			}
		}
		output1.setDimTable(subDimTable);

		// Set the outputs
		realOutputs.add(output1);

		// Return status
		return true;
	}

	public static ImageProcessor evaluate(List<DimensionMap> stack, JEXData image, boolean abs, boolean finalMinusInitial)
	{

		String pathToGet = ImageReader.readImagePath(image.getData(stack.get(0)), image.hasVirtualFlavor());
		ImageProcessor initial = (new ImagePlus(pathToGet)).getProcessor();
		int bitDepth = initial.getBitDepth();
		initial = initial.convertToFloatProcessor();		
		pathToGet = ImageReader.readImagePath(image.getData(stack.get(stack.size()-1)), image.hasVirtualFlavor());
		ImageProcessor ret = (new ImagePlus(pathToGet)).getProcessor();
		ret = ret.convertToFloatProcessor();
		
		Blitter b = null;
		if(finalMinusInitial)
		{
			b = new FloatBlitter((FloatProcessor) ret);
			b.copyBits(initial, 0, 0, Blitter.SUBTRACT);
		}
		else
		{
			b = new FloatBlitter((FloatProcessor) initial);
			b.copyBits(ret, 0, 0, Blitter.SUBTRACT);
			ret = initial;
		}
		
//		Blitter b = null;
//		if(bitDepth == 8)
//		{
//			b = new ByteBlitter((ByteProcessor) ret);
//		}
//		else if(bitDepth == 16)
//		{
//			b = new ShortBlitter((ShortProcessor) ret);
//		}
//		else if(bitDepth == 32)
//		{
//			b = new FloatBlitter((FloatProcessor) ret);
//		}
		
		ret.resetMinAndMax();
		if(abs)
		{
			((FloatProcessor) ret).abs();
		}
		if(ret.getMin() < 0 && bitDepth == 32)
		{
			return ret;
		}
		ret = JEXWriter.convertToBitDepthIfNecessary(ret, bitDepth);
		return ret;
	}

	public static ImageProcessor evaluate(List<DimensionMap> stack, JEXData image, String method)
	{
		ImagePlus virtualStack = ImageReader.readSomeOfObjectToVirtualStack(image, stack);

		return evaluate(virtualStack, method);
	}

	public static int getMethodInt(String method)
	{
		// :,mean,max,min,sum,std. dev.,median"///
		int methodInt = 5;
		if(method.equals(METHOD_MEAN))
			methodInt = 0;
		else if(method.equals(METHOD_MAX))
			methodInt = 1;
		else if(method.equals(METHOD_MIN))
			methodInt = 2;
		else if(method.equals(METHOD_SUM))
			methodInt = 3;
		else if(method.equals(METHOD_STDEV))
			methodInt = 4;
		else if(method.equals(METHOD_MEDIAN))
			methodInt = 5;
		return methodInt;
	}

	public static ImageProcessor evaluate(ImagePlus virtualStack, String method)
	{
		// :,mean,max,min,sum,std. dev.,median"///
		int methodInt = getMethodInt(method);

		ZProjector p = new ZProjector(virtualStack);
		p.setStartSlice(1);
		p.setStopSlice(virtualStack.getImageStackSize());
		p.setMethod(methodInt);
		p.doProjection();
		return p.getProjection().getProcessor();
	}

	private List<DimensionMap> getAllStackMaps(DimensionMap map, Dim dimToProject, DimTable filterTable)
	{
		List<DimensionMap> ret = new Vector<DimensionMap>();
		for (int i = 0; i < dimToProject.size(); i++)
		{
			DimensionMap temp = this.getAStackMap(map, dimToProject, i);
			if(!filterTable.testMapAsExclusionFilter(temp))
			{
				ret.add(temp);
			}
		}
		return ret;
	}

	private List<DimensionMap> getSomeStackMaps(DimensionMap map, Dim dimToProject, int slidingWindowSize, int i, DimTable filterTable)
	{
		Dim subDim = this.getSubDim(dimToProject, slidingWindowSize, i);
		List<DimensionMap> ret = new Vector<DimensionMap>();
		for (int j = 0; j < subDim.size(); j++)
		{
			DimensionMap temp = this.getAStackMap(map, subDim, j);
			if(!filterTable.testMapAsExclusionFilter(temp))
			{
				ret.add(temp);
			}
		}
		return ret;
	}

	private DimensionMap getAStackMap(DimensionMap map, Dim dimToProject, int indexOfValue)
	{
		DimensionMap ret = map.copy();
		ret.put(dimToProject.name(), dimToProject.valueAt(indexOfValue));
		return ret;
	}

	private Dim getSubDim(Dim dimToProject, int slidingWindowSize, int i)
	{
		Dim right = new Dim(dimToProject.name(), dimToProject.valuesStartingAt(i));
		Dim left = new Dim(dimToProject.name(), dimToProject.valuesUpThrough(i + slidingWindowSize - 1));
		return Dim.intersect(left, right);
	}
	
	private FloatProcessor multiplicationProjection(List<DimensionMap> stackMaps, JEXData imageData)
	{
		FloatProcessor ret = null;
		FloatBlitter fb = null;
		for(DimensionMap map : stackMaps)
		{
			String path = ImageReader.readImagePath(imageData.getData(map), imageData.hasVirtualFlavor());
			if(path != null)
			{
				FloatProcessor fp = (new ImagePlus(path)).getProcessor().convertToFloatProcessor();
				if(ret == null)
				{
					ret = fp;
					fb = new FloatBlitter(fp);
				}
				else
				{
					fb.copyBits(fp, 0, 0, Blitter.MULTIPLY);
				}
			}
		}
		return ret;
	}
}

// class AdjustImageHelperFunction implements GraphicalCrunchingEnabling,
// ImagePanelInteractor{
// ImagePanel imagepanel ;
// GraphicalFunctionWrap wrap ;
// DimensionMap[] dimensions ;
// String[] images ;
// ImagePlus im ;
// FloatProcessor imp ;
// int index = 0 ;
// int atStep = 0 ;
//
// boolean auto = false;
// double oldMin = 0;
// double oldMax = 1000;
// double newMin = 0;
// double newMax = 100;
// double gamma = 0.5;
// int depth = 16;
//
// ParameterSet params;
// JEXData imset;
// JEXEntry entry;
// String[] outputNames;
// public JEXData output;
//
// AdjustImageHelperFunction(JEXData imset, JEXEntry entry, String[]
// outputNames, ParameterSet parameters){
// // Pass the variables
// this.imset = imset;
// this.params = parameters;
// this.entry = entry;
// this.outputNames = outputNames;
//
// ////// Get params
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// TreeMap<DimensionMap,JEXDataSingle> map = imset.getDataMap();
// int length = map.size();
// images = new String[length];
// dimensions = new DimensionMap[length];
// int i = 0;
// for (DimensionMap dim: map.keySet()){
// JEXDataSingle ds = map.get(dim);
// String path = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
// ds.get(JEXDataSingle.FILENAME);
// dimensions[i] = dim;
// images[i] = path;
// i ++;
// }
//
// // Prepare the graphics
// imagepanel = new ImagePanel(this,"Adjust image");
// displayImage(index);
// wrap = new GraphicalFunctionWrap(this,params);
// wrap.addStep(0, "Select roi", new String[]
// {"Automatic","Old Min","Old Max","New Min","New Max","Output Bit Depth"});
// wrap.setInCentralPanel(imagepanel);
// wrap.setDisplayLoopPanel(true);
// }
//
// private void displayImage(int index){
// ImagePlus im = new ImagePlus(images[index]);
// imagepanel.setImage(im);
// }
//
// /**
// * Run the function and open the graphical interface
// * @return the ROI data
// */
// public JEXData doit(){
// if (!auto){
// wrap.start();
// }
// else {
// finishIT();
// }
//
// return output;
// }
//
// public void runStep(int index) {
// // Get the new parameters
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// // prepare the images for calculation
// ImagePlus im = new ImagePlus(images[index]);
// imagepanel.setImage(im);
// imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a
// float processor
//
// adjustImage();
//
// imagepanel.setImage(new ImagePlus("",imp));
// }
// public void runNext(){
// atStep = atStep+1;
// if (atStep > 0) atStep = 0;
// }
// public void runPrevious(){
// atStep = atStep-1;
// if (atStep < 0) atStep = 0;
// }
// public int getStep(){ return atStep;}
//
// public void loopNext(){
// index = index + 1;
//
// if (index >= images.length-1) index = images.length-1;
// if (index < 0) index = 0;
//
// runStep(index);
// }
// public void loopPrevious(){
// index = index - 1;
//
// if (index >= images.length-1) index = images.length-1;
// if (index < 0) index = 0;
//
// runStep(index);
// }
// public void recalculate(){}
//
// public void startIT() {
// wrap.displayUntilStep();
// }
// /**
// * Apply the roi to all other images
// */
// public void finishIT() {
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// output = new DefaultJEXData(JEXData.IMAGE,outputNames[0],"Adjusted image");
//
// // Run the function
// TreeMap<DimensionMap,JEXDataSingle> map = imset.getDataMap();
// int count = 0;
// int total = map.size();
// JEXStatics.statusBar.setProgressPercentage(0);
// for (DimensionMap dim: map.keySet()){
// JEXDataSingle ds = map.get(dim);
// String imagePath = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
// ds.get(JEXDataSingle.FILENAME);
// File imageFile = new File(imagePath);
// String imageName = imageFile.getName();
//
// // get the image
// im = new ImagePlus(imagePath);
// imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a
// float processor
//
// ////// Begin Actual Function
// adjustImage();
// ////// End Actual Function
//
// ////// Save the results
// String localDir = JEXStatics.jexManager.getLocalFolder(entry);
// String newFileName = FunctionUtility.getNextName(localDir, imageName, "A");
// String newImagePath = localDir + File.separator + newFileName;
// FunctionUtility.imSave(imp, "false", depth, newImagePath);
//
// JEXDataSingle outputds = new DefaultJEXDataSingle();
// outputds.put(JEXDataSingle.FOLDERNAME, localDir);
// outputds.put(JEXDataSingle.FILENAME, newFileName);
// output.addData(dim,outputds);
// Logs.log("Finished processing " + count + " of " + total +
// ".",1,this);
// count++;
//
// // Status bar
// int percentage = (int) (100 * ((double) count/ (double)map.size()));
// JEXStatics.statusBar.setProgressPercentage(percentage);
// }
//
//
// }
//
// private void adjustImage(){
// FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
// }
//
//
// public void clickedPoint(Point p) {}
// public void pressedPoint(Point p) {}
// public void mouseMoved(Point p){}
//
// }

