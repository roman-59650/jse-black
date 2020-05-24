package fr.ulille.spexp.math;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Locale;

public class FFTransformer {

    private static final int MAXRENDERINGPOINTS = 4096;
    private double[] ydata;
    private double[] amp;
    private double freq;
    private int size;

    public FFTransformer(double[] data, int size){
        this.size = size;
        DoubleFFT_1D fft = new DoubleFFT_1D(this.size);
        ydata = new double[2*this.size];
        amp = new double[this.size];
        System.arraycopy(data,0,ydata,0,this.size);
        fft.complexForward(ydata);
        amp[0] = 0;
        for (int i=1;i<this.size;i++){
            amp[i] = ydata[2*i];
        }
    }

    private double getMinValue(){
        double min = amp[0];
        for (int i=1;i<size;i++){
            if (amp[i]<min) min = amp[i];
        }
        return min;
    }

    private double getMaxValue(){
        double max = amp[0];
        for (int i=1;i<size;i++){
            if (amp[i]>max) max = amp[i];
        }
        return max;
    }

    private double[] getXFFTArray(GraphicsContext gc){
        double width = gc.getCanvas().getWidth();
        double[] xdata = new double[size];
        double span = 0.5;
        for (int i=0;i<size;i++){
            xdata[i] = ((double) i)/size*width/span;
        }
        return xdata;
    }

    public double[] getYFFTArray(GraphicsContext gc){
        double height = gc.getCanvas().getHeight();
        double[] ydata = new double[size];
        double ymin = getMinValue();
        double yspan = getMaxValue()-getMinValue();
        for (int i=0;i<size;i++){
            ydata[i] = height - height*(amp[i]-ymin)/yspan;
        }
        return ydata;
    }

    private void plotXY(GraphicsContext gc, double[] xdata, double[] ydata, int npoints, Color color){
        gc.setStroke(color);
        gc.beginPath();

        if (npoints>=MAXRENDERINGPOINTS) {
            double ix = xdata[0];
            final double DIX = 1./2.;  // plotting each 1/2 pixel
            double miny = ydata[0];
            double maxy = ydata[0];
            for (int i = 1; i < npoints; i++) {
                if ((xdata[i] >= ix) && (xdata[i] < ix + DIX)) {
                    if (ydata[i] > maxy) maxy = ydata[i];
                    if (ydata[i] < miny) miny = ydata[i];
                } else {
                    gc.lineTo(ix, miny);
                    gc.lineTo(ix, maxy);
                    ix = xdata[i];
                    miny = ydata[i];
                    maxy = ydata[i];
                }
            }
        }
        else {
            gc.moveTo(xdata[0],ydata[0]);
            for (int i = 1; i < npoints; i++) {
                gc.lineTo(xdata[i], ydata[i]);
            }
        }
        gc.stroke();
    }

    public void plot(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0,0, canvas.getWidth(), canvas.getHeight());
        double height = gc.getCanvas().getHeight();
        double width = gc.getCanvas().getWidth();
        double ymin = getMinValue();
        double yspan = getMaxValue()-getMinValue();
        double[] ixData = getXFFTArray(gc);
        double[] iyData = getYFFTArray(gc);

        gc.setFill(Color.WHITE);
        gc.fillRect(0,0,width,height);


        for (int i=1;i<10;i++){
            gc.setLineDashes(2);
            gc.setStroke(Color.LIGHTGRAY);
            gc.strokeLine(i*0.1*width,0,i*0.1*width,height);
            gc.strokeLine(0, height - height*0.1*i,width,height - height*0.1*i);

            if (i>0) {
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFont(new Font("Arial",12));
                gc.setTextBaseline(VPos.BOTTOM);
                gc.setStroke(Color.BLACK);
                gc.setFill(Color.BLACK);
                gc.setLineDashes(null);
                gc.strokeText(String.format(Locale.US, "%.1f", i * 0.1), i * 0.1 * width, height);
            }
        }

        gc.setLineDashes(0);
        plotXY(gc,ixData,iyData,size,Color.DARKCYAN);
    }

}
