package function.plugin.plugins.featureExtraction;

import java.util.TreeMap;
import java.util.TreeSet;

import function.ops.intervals.CroppedRealRAI;
import function.ops.intervals.IntersectedBooleanRAI;
import function.plugin.IJ2.IJ2PluginUtility;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.PointSamplerList;
import image.roi.ROIPlus;
import miscellaneous.Canceler;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.logic.RealLogic;
import net.imagej.ops.map.MapIterableIntervalToSamplingRAI;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Had to copy this class out temporarily while SNAPSHOTS conflict and this code is in flux.
 * Taken from imglib2-algorithm ConnectedComponents
 * 
 * @author Tobias Pietzsch
 *
 */
public class FeatureUtils {

	private UnaryFunctionOp<Object, Object> contourFunc;
	private UnaryFunctionOp<RealCursor<IntType>,Circle> cirOp;
	
	/////////////////////////////////////////
	//////// Connected Components ///////////
	/////////////////////////////////////////

	public <I extends IntegerType< I >> ImgLabeling<Integer, IntType> getConnectedComponents(final RandomAccessibleInterval<I> inputImg, boolean fourConnected)
	{
		StructuringElement se = null;
		if(fourConnected)
		{
			se = StructuringElement.FOUR_CONNECTED;
		}
		else
		{
			se = StructuringElement.EIGHT_CONNECTED;
		}

		long[] dimensions = new long[inputImg.numDimensions()];
		inputImg.dimensions(dimensions);
		final Img< IntType > indexImg = ArrayImgs.ints( dimensions );
		ImgLabeling<Integer, IntType> labeling = new ImgLabeling<Integer, IntType>(indexImg);
		ConnectedComponents.labelAllConnectedComponents(Views.offsetInterval(inputImg, inputImg), labeling, new LabelGenerator(), se);		

		return labeling;
	}

	public <T extends BooleanType<T>> ImgLabeling<Integer, IntType> getConnectedComponentsInRegion(RandomAccessibleInterval<T> reg, Img<UnsignedByteType> mask, boolean fourConnected)
	{
		CroppedRealRAI<T, UnsignedByteType> cropped = new CroppedRealRAI<>(reg, mask);
		IntervalView<UnsignedByteType> v = Views.offsetInterval(cropped, cropped);
		ImgLabeling<Integer, IntType> cellLabeling = this.getConnectedComponents(v, fourConnected);
		return cellLabeling;
	}
	
	/////////////////////////////////////////
	///////////// Show Images ///////////////
	/////////////////////////////////////////

	public void showRegion(LabelRegion<?> region)
	{
		this.showRegion(region, "Label = " + region.getLabel() + " at pos: " + ((int) (region.getCenterOfMass().getDoublePosition(0) + 0.5)) + ", " + ((int) (region.getCenterOfMass().getDoublePosition(1) + 0.5)));
	}

	public <T extends BooleanType<T>> void showRegion(RandomAccessibleInterval<T> region)
	{
		this.showRegion(region, "Region");
	}

	public <T extends BooleanType<T>> void showRegion(RandomAccessibleInterval<T> region, String title)
	{
		ImageJFunctions.showUnsignedByte(region, new BooleanTypeToUnsignedByteTypeConverter<T>(), title);
	}

	public <T extends RealType<T>> void show(RandomAccessibleInterval< T > rai)
	{
		this.show(rai, false);
	}

	public <T extends RealType<T>> void show(RandomAccessibleInterval< T > rai, boolean defaultApp)
	{
		FileUtility.showImg(rai, defaultApp);
	}
	
	public void showVoidII(IterableInterval< Void > region, boolean defaultApp)
	{
		this.show(this.makeImgFromVoidII(region), defaultApp);
	}

	public void showVoidII(IterableInterval< Void > region)
	{
		this.showVoidII(region, false);
	}
	
	public <T extends RealType<T>>void showRealII(IterableInterval< T > region, boolean defaultApp)
	{
		this.show(this.makeImgFromRealII(region), defaultApp);
	}
	
	public <T extends RealType<T>>void showRealII(IterableInterval< T > region)
	{
		this.showRealII(region, true);
	}

	public void show(ImgLabeling<Integer,IntType> labeling)
	{
		this.show(labeling, false);
	}

	public void show(ImgLabeling<Integer,IntType> labeling, boolean asMask)
	{
		if(asMask)
		{
			FileUtility.showImg(this.makeImgMaskFromLabeling(labeling), false);
		}
		else
		{
			FileUtility.showImg(this.makeImgFromLabeling(labeling), true);
		}
	}

	/////////////////////////////////////////
	///////////// Make Images ///////////////
	/////////////////////////////////////////

	public Img<UnsignedShortType> makeImgFromShortII(IterableInterval<UnsignedShortType> ii)
	{
		long[] dims = new long[ii.numDimensions()];
		ii.dimensions(dims);
		Img<UnsignedShortType> img = ArrayImgs.unsignedShorts(dims);
		Cursor<UnsignedShortType> c = ii.cursor();
		RandomAccess<UnsignedShortType> ra = img.randomAccess();
		while(c.hasNext())
		{
			c.fwd();
			ra.setPosition(c);
			ra.get().set(c.get());
		}
		return img;
	}

	public Img<UnsignedByteType> makeImgFromByteII(IterableInterval<UnsignedByteType> ii)
	{
		long[] dims = new long[ii.numDimensions()];
		ii.dimensions(dims);
		Img<UnsignedByteType> img = ArrayImgs.unsignedBytes(dims);
		Cursor<UnsignedByteType> c = ii.cursor();
		RandomAccess<UnsignedByteType> ra = img.randomAccess();
		while(c.hasNext())
		{
			c.fwd();
			ra.setPosition(c);
			ra.get().set(c.get());
		}
		return img;
	}

	public Img<UnsignedByteType> makeImgFromByteRAI(RandomAccessibleInterval<UnsignedByteType> src)
	{
		RandomAccessibleInterval<UnsignedByteType> rai = Views.offsetInterval(src, src);
		long[] dims = new long[rai.numDimensions()];
		rai.dimensions(dims);
		Img<UnsignedByteType> img = ArrayImgs.unsignedBytes(dims);
		Cursor<UnsignedByteType> c = img.cursor();
		RandomAccess<UnsignedByteType> ra = rai.randomAccess();
		while(c.hasNext())
		{
			c.fwd();
			ra.setPosition(c);
			c.get().set(ra.get());
		}
		return img;
	}
	
	public <T extends RealType<T>> Img<UnsignedShortType> makeImgFromRealII(IterableInterval< T > region)
	{
		long[] dimensions = new long[region.numDimensions()];
		region.dimensions(dimensions);
		final Img<UnsignedShortType> ret = ArrayImgs.unsignedShorts( dimensions );
		Cursor<T> c = region.cursor();
		Point min = new Point(0,0);
		Point max = new Point(0,0);
		Point cur = new Point(0,0);
		region.min(min);
		region.max(max);
		RandomAccess<UnsignedShortType> ra = ret.randomAccess();
		//System.out.println(min + ", " + max);
		while(c.hasNext())
		{
			c.fwd();
			//System.out.println("" + (c.getIntPosition(0)-min.getIntPosition(0)) + " , " + (c.getIntPosition(1)-min.getIntPosition(1)));
			cur.setPosition(c.getIntPosition(0)-min.getIntPosition(0), 0); 
			cur.setPosition(c.getIntPosition(1)-min.getIntPosition(1), 1);
			ra.setPosition(cur);
			ra.get().setReal(c.get().getRealDouble());
		}
		return ret;
	}

	public Img<UnsignedByteType> makeImgFromVoidII(IterableInterval< Void > region)
	{
		long[] dimensions = new long[region.numDimensions()];
		region.dimensions(dimensions);
		final Img<UnsignedByteType> ret = ArrayImgs.unsignedBytes( dimensions );
		Cursor<Void> c = region.cursor();
		Point min = new Point(0,0);
		Point max = new Point(0,0);
		Point cur = new Point(0,0);
		region.min(min);
		region.max(max);
		RandomAccess<UnsignedByteType> ra = ret.randomAccess();
		//System.out.println(min + ", " + max);
		while(c.hasNext())
		{
			c.fwd();
			//System.out.println("" + (c.getIntPosition(0)-min.getIntPosition(0)) + " , " + (c.getIntPosition(1)-min.getIntPosition(1)));
			cur.setPosition(c.getIntPosition(0)-min.getIntPosition(0), 0); 
			cur.setPosition(c.getIntPosition(1)-min.getIntPosition(1), 1);
			ra.setPosition(cur);
			ra.get().set(255);
		}
		return ret;
	}

	public Img<UnsignedByteType> makeImgMaskFromLabeling(ImgLabeling<Integer,IntType> labeling)
	{
		LabelRegions<Integer> regions = new LabelRegions<>(labeling);
		long[] dims = new long[labeling.numDimensions()];
		labeling.dimensions(dims);
		final Img< UnsignedByteType > ret = ArrayImgs.unsignedBytes(dims);
		RandomAccess<UnsignedByteType> ra = ret.randomAccess();
		for(LabelRegion<Integer> region : regions)
		{
			Cursor<Void> c = region.cursor();
			while(c.hasNext())
			{
				c.fwd();
				ra.setPosition(c);
				ra.get().set(255);
			}
		}
		return ret;
	}

	public Img<UnsignedShortType> makeImgFromLabeling(ImgLabeling<Integer,IntType> labeling)
	{
		LabelRegions<Integer> regions = new LabelRegions<>(labeling);
		long[] dims = new long[labeling.numDimensions()];
		labeling.dimensions(dims);
		final Img< UnsignedShortType > ret = ArrayImgs.unsignedShorts(dims);
		RandomAccess<UnsignedShortType> ra = ret.randomAccess();
		for(LabelRegion<Integer> region : regions)
		{
			Cursor<Void> c = region.cursor();
			while(c.hasNext())
			{
				c.fwd();
				ra.setPosition(c);
				ra.get().setInteger(region.getLabel());
			}
		}
		return ret;
	}
	
	/////////////////////////////////////////
	///////// Geometry Functions ////////////
	/////////////////////////////////////////

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends BooleanType<T>> Polygon getPolygonFromBoolean(final RandomAccessibleInterval<T> src) {
		if (contourFunc == null) {
			contourFunc = (UnaryFunctionOp) Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.Contour.class, Polygon.class, src, true, true);
		}
		final Polygon p = (Polygon) contourFunc.compute1(src);
		return p;
	}

	public <T extends RealType<T>> Polygon getPolygonFromReal(final RandomAccessibleInterval<T> src)
	{
		return this.getPolygonFromBoolean(this.convertRealToBoolTypeRAI(src));
	}
	
	/**
	 * If center is null the circle is the smallest enclosing circle.
	 * If not, the circle is the smallest enclosing circle with a center at 'center'.
	 * @param pg
	 * @param center
	 * @return
	 */
	public Circle getCircle(Polygon pg, RealLocalizable center)
	{
		PointSamplerList<IntType> psl = new PointSamplerList<>(pg.getVertices(), new IntType(0));
		if(this.cirOp == null)
		{
			cirOp = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, psl.cursor(), center);
		}
		UnaryFunctionOp<RealCursor<IntType>,Circle> cirOp = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, psl.cursor(), center);
		return cirOp.compute1(psl.cursor());
	}
	
	public <T extends BooleanType<T>> Circle getCircle(RandomAccessibleInterval<T> region, RealLocalizable center)
	{
		Polygon p = this.getPolygonFromBoolean(region);
		return this.getCircle(p, center);
	}
	
	/////////////////////////////////////////
	//////////// Sub-routines ///////////////
	/////////////////////////////////////////
	
	public Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>> keepRegionsWithMaxima(Img<UnsignedByteType> mask, boolean fourConnected, ROIPlus maxima, Canceler canceler)
	{
		// Create a blank image
		ArrayImgFactory<UnsignedByteType> factory = new ArrayImgFactory<UnsignedByteType>();
		long[] dims = new long[mask.numDimensions()];
		mask.dimensions(dims);
		Img<UnsignedByteType> blank = factory.create(dims, new UnsignedByteType(0));

		// Get the regions
		ImgLabeling<Integer, IntType> labeling = this.getConnectedComponents(mask, fourConnected);
		//		ImageJFunctions.show(mask);
		LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);

		TreeMap<Integer, PointList> labelToPointsMap = new TreeMap<Integer,PointList>();
		TreeSet<Integer> labelsToCopy = new TreeSet<Integer>();

		// For each region, if contains a maxima, "copy" the region to the blank image 
		for(Integer label : regions.getExistingLabels())
		{
			if(canceler != null && canceler.isCanceled())
			{
				return null;
			}
			LabelRegion<Integer> region = regions.getLabelRegion(label);
			//Polygon poly = convert(region);
			for(IdPoint p : maxima.getPointList())
			{
				if(canceler != null && canceler.isCanceled())
				{
					return null;
				}
				if(this.contains(region, p)) //poly.contains(p))
				{
					PointList pl = labelToPointsMap.get(label);
					if(pl == null)
					{
						pl = new PointList();
					}
					pl.add(p.copy());
					labelToPointsMap.put(label, pl);
					labelsToCopy.add(label);
				}
			}
		}

		for(Integer label : labelsToCopy)
		{
			LabelRegion<Integer> region = regions.getLabelRegion(label);
			//			ImageJFunctions.show(new SamplingIterableRegion(region, mask));
			Op op = IJ2PluginUtility.ij().op().op(RealLogic.Or.class, RealType.class, RealType.class);
			IJ2PluginUtility.ij().op().run(MapIterableIntervalToSamplingRAI.class, blank, Regions.sample(region, mask), op);
		}

		Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>> ret = new Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>>();
		ret.p1 = blank;
		ret.p2 = labelToPointsMap;
		return ret;
	}
	
	/////////////////////////////////////////
	///////// Region Manipulation ///////////
	/////////////////////////////////////////
	
	public <T extends BooleanType<T>, R extends RealType<R>> RandomAccessibleInterval<R> cropRealRAI(RandomAccessibleInterval<T> region, RandomAccessibleInterval<R> img)
	{
		return new CroppedRealRAI<T,R>(region, img);
	}

	public boolean contains(LabelRegion<?> region, IdPoint p)
	{
		// Use the random access to directly determine if the label region contains the point.
		// Should be faster than iterating
		RandomAccess<BoolType> ra = Regions.iterable(region).randomAccess();
		ra.setPosition(p);
		return ra.get().get();
	}

	public <T extends BooleanType<T>> IterableRegion<T> intersect(IterableRegion<T> a, IterableRegion<T> b)
	{
		RandomAccessibleInterval<T> temp = intersect(a, b);
		return Regions.iterable(temp);
	}

	public <T extends BooleanType<T>> RandomAccessibleInterval<T> intersect(RandomAccessibleInterval<T> a, RandomAccessibleInterval<T> b)
	{
		return new IntersectedBooleanRAI<T>(a, b);
	}

	/////////////////////////////////////////
	////////////// Conversion ///////////////
	/////////////////////////////////////////
	
	public <T extends BooleanType<T>> RandomAccessibleInterval<UnsignedByteType> convertBooleanTypeToByteRAI(RandomAccessibleInterval<T> rai)
	{
		return new ConvertedRandomAccessibleInterval<T, UnsignedByteType>(rai, new BooleanTypeToUnsignedByteTypeConverter<T>(), new UnsignedByteType(0));
	}

	public <R extends RealType<R>> RandomAccessibleInterval<BoolType> convertRealToBoolTypeRAI(RandomAccessibleInterval<R> rai)
	{
		return new ConvertedRandomAccessibleInterval<>(rai, new RealTypeToBoolTypeConverter<R>(), new BoolType(false));
	}

	class VoidToBitTypeConverter implements Converter<Void, BitType> {

		@Override
		public void convert(Void input, BitType output) {
			output.set(true);
		}
	}

	class BooleanTypeToUnsignedByteTypeConverter<T extends BooleanType<T>> implements Converter<T, UnsignedByteType> {

		@Override
		public void convert(T input, UnsignedByteType output) {
			if(input.get())
			{
				output.set(255);
			}
			else
			{
				output.set(0);
			}

		}
	}

	class RealTypeToBoolTypeConverter<R extends RealType<R>> implements Converter<R, BoolType> {

		@Override
		public void convert(R input, BoolType output) {
			if(input.getRealDouble() > 0)
			{
				output.set(true);
			}
			else
			{
				output.set(false);
			}

		}
	}
}


