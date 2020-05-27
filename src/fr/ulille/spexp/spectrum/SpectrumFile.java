package fr.ulille.spexp.spectrum;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author roman
 */
public class SpectrumFile {

    protected int dataPoints;
	protected double freqMin;
	protected double freqMax;
	protected double YMin;
	protected double YMax;
	protected double[] xData;
	protected double[] yData;
	protected boolean fileRead;
    protected int xaLimit;
    protected int xbLimit;
    protected double ycLimit;
    protected double ydLimit;
    protected double iniyc;
    protected double iniyd;
    protected double ydOffset;
    //protected double fStep;
    protected String fileName;
    private List<Double> xdataList;
    private List<Double> ydataList;

    public void scaleData(){
        double dmin = yData[xaLimit];
        double dmax = yData[xaLimit];
        for (int i=xaLimit;i<xbLimit;i++){
            if (yData[i]>dmax) dmax = yData[i];
            if (yData[i]<dmin) dmin = yData[i];
        }
        ycLimit = dmin-0.05*(dmax-dmin);
        ydLimit = dmax+(0.05+ydOffset)*(dmax-dmin);
    }

    public int getSize(){
        return dataPoints;
    }

    public int getXA(){
        return xaLimit;
    }

  	public int getXB(){
        return xbLimit;
    }

	public int getScreenSize(){
        return (xbLimit-xaLimit+1);
    }

    /**
     *
      * @return minimum frequency value for a given file
     */
    public double getFreqMin(){
        return freqMin;
    }

    /**
     *
      * @return maximum frequency value for a given file
     */
	public double getFreqMax(){
	    return freqMax;
	}

    /**
     *
      * @return frequency span for a given file
     */
    public double getSpan(){
        return freqMax-freqMin;
    }

	public double getYMin(){
              return YMin;
          }

	public double getYMax(){
              return YMax;
          }

    /**
     *
      * @return minimum frequency value for a screen
     */
	public double getScreenFreqMin(){
	    return xData[xaLimit];
	}

	public double getScreenFreqMax(){
              return xData[xbLimit];
          }

    public double getScreenSpan(){
              return xData[xbLimit]-xData[xaLimit];
          }

	public double getYScreenMin(){
              return ycLimit;
	}

	public double getYScreenMax(){
              return ydLimit;
          }

    public void setYOffset(double offset){
        ydOffset = offset;
        scaleData();
    }

    public double getXData(int i){
		if (i>=0&&i<dataPoints) return xData[i];
		else return 0;
    }

    public double[] getXData() {
        return xData;
    }

    public double getYData(int i){
        if (i>=0&&i<dataPoints) return yData[i];
        else return 0;
    }

    public double[] getYData(){
	    return yData;
    }

    public long getDataIndex(double f){
        return Math.round((dataPoints-1)*(f-freqMin)/(freqMax-freqMin));
    }

    public boolean isFileRead(){ return fileRead; }

    public void setXScreenLimits(int imin, int imax){
        if (imax>imin){
            if (imin>=0) xaLimit = imin;
            else xaLimit = 0;
            if (imax<=dataPoints)xbLimit = imax;
            else xbLimit=dataPoints;
            scaleData();
        }
    }

    public void setXScreenLimits(double fmin, double fmax){
	    long imin = getDataIndex(fmin);
	    long imax = getDataIndex(fmax);
        if (imax>imin){
            if (imin>=0) xaLimit = (int) imin;
            else xaLimit = 0;
            if (imax<=dataPoints) xbLimit = (int) imax;
            else xbLimit=dataPoints;
            scaleData();
        }
    }

    public void vertZoomIn(double factor){
        ycLimit *= factor;
        ydLimit *= factor;
    }

    public void vertZoomOut(double factor){
        ycLimit /= factor;
        ydLimit /= factor;
    }

    public void vertZoom(double factor){
	    ycLimit = iniyc/factor;
	    ydLimit = iniyd/factor;
    }

    protected void splitString(String s, double[] v) {
        String[] sp = s.split("\\s");
        int i = 0;
        for (String w : sp) {
            if ((w.length() > 1) && (i < v.length)) {
                v[i] = Double.parseDouble(w);
                i++;
            }
        }
    }

    public void readFileList(List<String> files, double fmin, double fmax) throws IOException {
	    fileRead = false;
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        List<Double> xlo = new ArrayList<>();
        List<Double> ylo = new ArrayList<>();
        List<Double> xup = new ArrayList<>();
        List<Double> yup = new ArrayList<>();
        List<String> inputList = new ArrayList<>();
        for (String file : files){
            BufferedReader inputStream = new BufferedReader(new FileReader(file));
            String line = inputStream.readLine();
            while (line!=null){
                line = inputStream.readLine();
                if (line!=null) inputList.add(line);
            }
            int size = inputList.size();
            double[] xy = new double[2];
            for (int i=0;i<size;i++) {
                if (!inputList.get(i).isEmpty()) splitString(inputList.get(i), xy);
                if (xy[0]>=fmin && xy[0]<=fmax){
                    x.add(xy[0]);
                    y.add(xy[1]);
                }
            }
            inputList.clear();
        }
        double df = x.get(1)-x.get(0);
        if (x.get(0)-df>fmin){
            long steps = Math.round((x.get(0)-fmin)/df);
            for (int i=0;i<steps;i++){
                xlo.add(fmin+i*df);
                ylo.add(0.);
            }
        }
        if (x.get(x.size()-1)+df<fmax){
            long steps = Math.round((fmax-x.get(x.size()-1))/df);
            for (int i=0;i<steps;i++){
                xup.add(x.get(x.size()-1)+i*df);
                yup.add(0.);
            }
        }
        List<Double> xfull = Stream.of(xlo,x,xup).flatMap(s->s.stream()).collect(Collectors.toList());
        List<Double> yfull = Stream.of(ylo,y,yup).flatMap(s->s.stream()).collect(Collectors.toList());
        xData = xfull.stream().mapToDouble(Double::doubleValue).toArray();
        yData = yfull.stream().mapToDouble(Double::doubleValue).toArray();

        dataPoints = xData.length;
        freqMin = xData[0];
        freqMax = xData[dataPoints-1];
        //fStep = (xData[dataPoints - 1]-xData[0])/(dataPoints - 1);
        xaLimit = 0;
        xbLimit = dataPoints-1;
        YMin = Arrays.stream(yData).min().getAsDouble();
        YMax = Arrays.stream(yData).max().getAsDouble();
        ycLimit = YMin-0.05*(YMax-YMin);
        ydLimit = YMax+0.05*(YMax-YMin);
        iniyc = ycLimit;
        iniyd = ydLimit;
        fileRead = true;
    }

    public void readData(String fileName){
        try
        {
            List<String> inputList = new ArrayList<>();

            //FileInfo info = new FileInfo(fileName);

            xdataList = new ArrayList<>();
            ydataList = new ArrayList<>();

            // we determine the number of points to read
	        fileRead = false;
	    	BufferedReader inputStream = new BufferedReader(new FileReader(fileName));
	        String line;
            line = inputStream.readLine(); // read header line

            double[] xy = new double[2];
	        while (line != null)           // read all other lines
	        {
	           line = inputStream.readLine();
	           //if (line!=null) inputList.add(line);
	           if (line!=null&&!line.isEmpty()){
	               splitString(line,xy);
                   xdataList.add(xy[0]);
                   ydataList.add(xy[1]);
                   if (xy[1]<YMin) YMin = xy[1];
                   if (xy[1]>YMax) YMax = xy[1];
               }
	        }
	        //dataPoints = inputList.size();
	        inputStream.close();
	        // now we allocate memory for the data to be read
	        //xData = new double[dataPoints];
	        //yData = new double[dataPoints];
                

	        /*for (int i=0;i<dataPoints;i++){
	            if (!inputList.get(i).isEmpty()) splitString(inputList.get(i),xy);
	            //xData[i] = xy[0];
	            //yData[i] = xy[1];
	            //if (yData[i]>YMax) YMax = yData[i];
	            //if (yData[i]<YMin) YMin = yData[i];
                xdataList.add(xy[0]);
                ydataList.add(xy[1]);
	        }*/

	        inputList.clear();
	        fileRead = true;
	        this.fileName = fileName;

	        //YMax = ydataList.stream().max(Double::compare).get();
	        //YMin = ydataList.stream().min(Double::compare).get();
	        freqMin = xdataList.get(0);
	        freqMax = xdataList.get(xdataList.size()-1);
	        //fStep = (freqMax-freqMin)/(xdataList.size()-1);
            xaLimit = 0;
            xbLimit = xdataList.size()-1;
            ycLimit = YMin-0.05*(YMax-YMin);
            ydLimit = YMax+0.05*(YMax-YMin);
            iniyc = ycLimit;
            iniyd = ydLimit;
            xData = xdataList.stream().mapToDouble(Double::doubleValue).toArray();
            yData = ydataList.stream().mapToDouble(Double::doubleValue).toArray();
            dataPoints = xData.length;

	        /*freqMin = xData[0];
	        freqMax = xData[dataPoints-1];
	        fStep = (xData[dataPoints - 1]-xData[0])/(dataPoints - 1);
	        xaLimit = 0;
	        xbLimit = dataPoints-1;
	        ycLimit = YMin-0.05*(YMax-YMin);
	        ydLimit = YMax+0.05*(YMax-YMin);
	        iniyc = ycLimit;
	        iniyd = ydLimit;*/
        }
        catch(FileNotFoundException e){
            System.out.println("File "+fileName+" was not found");
	        System.out.println("or could not be opened.");
        }
        catch(IOException e){
	       System.out.println("Error reading from "+fileName);
        }
    }

    public void readKharkivData(String fileName) throws IOException {
        BufferedReader inputStream = new BufferedReader(new FileReader(fileName));
        String line;
        line = inputStream.readLine(); // read header line
        line = inputStream.readLine(); // read line #2
        double bfreq = Double.parseDouble(line.substring(0,13));
        double sfreq = Double.parseDouble(line.substring(15,24));
        line = inputStream.readLine(); // read line #3
        int nblocks = 0;
        int npoints = 0;
        if (line.substring(1,6).contains("Fulpow")){
            nblocks = 0;
            npoints = Integer.parseInt(line.substring(25,29).trim());
        } else {
            nblocks = Integer.parseInt(line.substring(9,13).trim());
            npoints = Integer.parseInt(line.substring(24,28).trim());
        }
        dataPoints = 256*nblocks+npoints;
        inputStream.readLine(); // read line #4
        inputStream.readLine(); // read line #5
        int skipfp = 0; // number of line to skip (full power data)
        if (nblocks%10==0)
            skipfp = nblocks/10;
        else
            skipfp = nblocks/10+1;
        for (int i=0;i<skipfp;i++)
            inputStream.readLine();
        xData = new double[dataPoints];
        yData = new double[dataPoints];
        for (int i=0;i<dataPoints;i++){
            xData[i] = bfreq+i*sfreq;
            yData[i] = Double.parseDouble(inputStream.readLine());
            if (yData[i]>YMax) YMax = yData[i];
            if (yData[i]<YMin) YMin = yData[i];
        }
        inputStream.close();
        fileRead = true;
        this.fileName = fileName;
        freqMin = xData[0];
        freqMax = xData[dataPoints-1];
        //fStep = (xData[dataPoints - 1]-xData[0])/(dataPoints - 1);
        xaLimit = 0;
        xbLimit = dataPoints-1;
        ycLimit = YMin-0.05*(YMax-YMin);
        ydLimit = YMax+0.05*(YMax-YMin);
        iniyc = ycLimit;
        iniyd = ydLimit;
    }

    public void WriteData(String fileName){
        if (!fileRead) return;
        FileWriter fout = null;
        try {
	    	fout = new FileWriter(fileName);
        } catch (IOException e) {
	     	e.printStackTrace();
        }
        String str;
        try {
        	fout.write("file saved: "+fileName+"\r\n");
        	for (int i=0;i<dataPoints;i++){
        		str = String.format(Locale.US,"%1$15.8f %2$7.1f",xData[i],yData[i]);
        		fout.write(str+"\r\n");
        	}
        } catch (IOException e) {
			e.printStackTrace();
        }
        try {
			fout.close();
        } catch (IOException e) {
			e.printStackTrace();
        }
    }

}
