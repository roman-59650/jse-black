package fr.ulille.spexp.spectrum;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileInfo {

    private double minfrequency;
    private double maxfrequency;
    private int size;
    private double step;
    private String molname;
    private String filename;

    public FileInfo(String name, double minfreq, double maxfreq){
        this.filename = name;
        this.minfrequency = minfreq;
        this.maxfrequency = maxfreq;
    }

    public FileInfo(String filename){
        try {
            if (filename.endsWith(".dat")) { // reading standard .dat files with 1 header line
                this.filename = filename;
                BufferedReader inputStream = new BufferedReader(new FileReader(filename));
                String s = inputStream.readLine();
                String[] data = s.split("\\|");
                molname = data[0];
                double[] xy = new double[2];
                splitString(inputStream.readLine(), xy);
                minfrequency = xy[0];
                splitString(inputStream.readLine(), xy);
                step = xy[0] - minfrequency;
                List<String> list = Files.readAllLines(new File(filename).toPath(), Charset.defaultCharset());
                size = list.size() - 1;
                maxfrequency = minfrequency + step * (size - 1);
            }
            if (filename.endsWith(".000")){
                this.filename = filename;
                BufferedReader inputStream = new BufferedReader(new FileReader(filename));
                String line;
                line = inputStream.readLine(); // read header line
                molname = line.substring(0,11).trim();
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
                int dataPoints = 256*nblocks+npoints;
                minfrequency = bfreq;
                step = sfreq;
                size = dataPoints;
                maxfrequency = minfrequency + step * (size - 1);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void splitString(String s, double[] v) {
        String[] sp = s.split("\\s");
        int i = 0;
        for (String w : sp) {
            if ((w.length() > 1) && (i < v.length)) {
                v[i] = Double.parseDouble(w);
                i++;
            }
        }
    }

    public String getFilename(){
        return filename;
    }

    public double getStep() {
        return step;
    }

    public double getMaxfrequency() {
        return maxfrequency;
    }

    public double getMinfrequency() {
        return minfrequency;
    }

    @Override
    public String toString() {
        Path p = Paths.get(this.filename);
        return p.getFileName().toString();
    }
}
