package function.plugin.old;

import java.util.HashMap;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author jaywarrick
 * 
 */
public class JEX_ImageTools_SliceROI extends JEXCrunchable {

	public JEX_ImageTools_SliceROI()
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
		String result = "Slice ROI";
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
		String result = "Grab once slice (i.e., all rois holding one dimension constant) to create a subset of ROI objects";
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
		String toolbox = "Image tools";
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
		inputNames[0] = new TypeName(ROI, "Roi");
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
		this.defaultOutputNames = new TypeName[0];
		// this.defaultOutputNames[0] = new TypeName(IMAGE, "Split Image");

		if(this.outputNames == null)
		{
			return this.defaultOutputNames;
		}
		return this.outputNames;
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
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p1 = new Parameter("Dim to Slice", "Name of the dimension to split", "Time");
		Parameter p2 = new Parameter("Value to Grab", "Value of the named dimension to be chosen/sliced from the rest", "1");
		Parameter p3 = new Parameter("Keep Dim?", "Keep the singleton dimension in the resultant object?", Parameter.CHECKBOX, false);

		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
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
		return false;
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
		JEXData roiData = inputs.get("Roi");
		roiData.getDataMap();
		if(roiData == null || !roiData.getTypeName().getType().matches(JEXData.ROI))
		{
			return false;
		}

		// Gather parameters
		String dim = this.parameters.getValueOfParameter("Dim to Slice");
		String value = this.parameters.getValueOfParameter("Value to Grab");
		DimensionMap filterMap = new DimensionMap(dim + "=" + value);
		Boolean keep = Boolean.parseBoolean(this.parameters.getValueOfParameter("Keep Dim?"));

		// Run the function
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		int count = 0, percentage = 0;
		TreeMap<DimensionMap,ROIPlus> splitRoiMap = new TreeMap<>();
		for (DimensionMap map : roiData.getDimTable().getSubTable(filterMap).getMapIterator())
		{
			ROIPlus roi = roiMap.get(map);
			if(roi == null)
			{
				continue;
			}
			if(keep)
			{
				splitRoiMap.put(map.copy(), roi);
			}
			else
			{
				DimensionMap newMap = map.copy();
				newMap.remove(dim);
				splitRoiMap.put(newMap.copy(), roi);
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) roiMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		JEXData output = RoiWriter.makeRoiObject(roiData.name + " " + dim + " " + value, splitRoiMap);
		this.realOutputs.add(output);

		if(this.realOutputs.size() == 0)
		{
			return false;
		}

		// Return status
		return true;
	}
}
