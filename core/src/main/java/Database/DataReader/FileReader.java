package Database.DataReader;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.SingleUserDatabase.JEXWriter;

import java.io.File;
import java.util.TreeMap;

import miscellaneous.FileUtility;
import tables.DimensionMap;

public class FileReader {
	
	public static String readToPath(JEXDataSingle ds)
	{
		return readToPath(ds, false);
	}
	
	public static String readToPath(JEXDataSingle ds, boolean virtual)
	{
		if (ds == null) return null;
		
		String relativePath = FileUtility.makeOSCompatible(ds.get(JEXDataSingle.RELATIVEPATH));
		if(virtual)
		{
			return relativePath;
		}
		else
		{
			String ret = JEXWriter.getDatabaseFolder() + File.separator + relativePath;
			return ret;
		}
	}
	
	public static String readToPath(String dataFolder, JEXDataSingle ds)
	{
		// This works for both real and virtual objects.
		String fileName = FileUtility.getFileNameWithExtension(ds.get(JEXDataSingle.RELATIVEPATH));
		String result = dataFolder + File.separator + fileName;
		return result;
	}
	
	public static File readToFile(JEXDataSingle ds)
	{
		return readToFile(ds, false);
	}
	
	public static File readToFile(JEXDataSingle ds, boolean virtual)
	{
		return new File(FileReader.readToPath(ds, virtual));
	}
	
	/**
	 * Get the file path stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static String readFileObject(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.FILE))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String ret = readToPath(ds, data.hasVirtualFlavor());
		
		return ret;
	}
	
	/**
	 * Get the file path stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static File readFileObjectToFile(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.FILE))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String result = readToPath(ds, data.hasVirtualFlavor());
		File file = new File(result);
		return file;
	}
	
	/**
	 * Read all the images in the value object into a hashable table of image paths
	 * 
	 * @param data
	 * @return
	 */
	public static TreeMap<DimensionMap,String> readObjectToFilePathTable(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.FILE))
			return null;
		TreeMap<DimensionMap,String> result = new TreeMap<DimensionMap,String>();
		JEXDataSingle ds = data.getFirstSingle();
		String dataFolder = FileReader.readToFile(ds, data.hasVirtualFlavor()).getParent(); // DO THIS
		// ONE TIME
		// OUTSIDE
		// LOOP
		// OTHERWISE
		// YOU WILL
		// CHECK IF
		// THIS
		// DIRECTORY
		// EXISTS FOR
		// EACH
		// DATASINGLE
		// IN THE
		// JEXDATA!
		// (MAJORLY
		// SLOW)
		for (DimensionMap map : data.getDataMap().keySet())
		{
			ds = data.getData(map);
			String path = readToPath(dataFolder, ds);
			result.put(map, path);
		}
		return result;
	}
	
}
