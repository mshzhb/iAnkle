package edu.utoronto.cimsah.myankle.Helpers;

import android.util.Log;

import java.util.ArrayList;

public class Samples {
    static String TAG = Samples.class.getSimpleName();
    private static final int X = 0, Y = 1, Z = 2, XNEG = 3, YNEG = 4, ZNEG = 5;
    private final ArrayList<Float> m_calibrationValues;
    private ArrayList<Sample> m_samples;
    private float xSum, ySum, zSum;

    public static final float GRAVITY = 9.81f;

    public Samples(ArrayList<Float> calibrationValues) {
        m_samples = new ArrayList<Sample>();

        xSum = 0;
        ySum = 0;
        zSum = 0;

        assert (calibrationValues != null && calibrationValues.size() == 6);

        if (containsZero(calibrationValues))
            m_calibrationValues = getDefaultCalValues();

        else
            m_calibrationValues = calibrationValues;
    }

    /*
    *Sets calibration values to all be g (i.e. no calibration effect will occur)
    */
    private ArrayList<Float> getDefaultCalValues() {
        ArrayList<Float> cal = new ArrayList<>(6);

        for (int x = 0; x < 6; x++)
            cal.add(GRAVITY);

        return cal;
    }

    /**
     * Checks if float arraylist contains zero
     */
    public boolean containsZero(ArrayList<Float> arrayList) {
        for (float f : arrayList)
            if (f == 0f)
                return true;

        return false;
    }

    /**
     * Adds a new Sample object to m_samples ArrayList
     *
     * @param t time
     * @param x
     * @param y
     * @param z
     */
    public void add(float t, float x, float y, float z) {
        if (x == 0 && y == 0 && z == 0){
            Log.e(TAG, "Null sample registered; sample was skipped");
        }
        else {
            Sample new_Sample = new Sample(t, x, y, z);
            m_samples.add(new_Sample);
            xSum += new_Sample.x_cal();
            ySum += new_Sample.y_cal();
            zSum += new_Sample.z_cal();
        }
    }

    /**
     * Clears all data; calibration notwithstanding
     */
    public void clear() {
        m_samples.clear();
        xSum = 0;
        ySum = 0;
        zSum = 0;
    }

    public int getNumSamples() {
        return m_samples.size();
    }

    /**
     * Computes the magnitude of an n dimensional vector, given all its components
     *
     * @param components Value of each component in vector
     * @return Magnitude of vector
     */
    public float vectorMagnitude(float... components) {
        float magnitude = 0;
        float sumOfSquares = 0;

        for (float curComponent : components)
            sumOfSquares += Math.pow(curComponent, 2);

        magnitude = (float) Math.sqrt(sumOfSquares);
        return magnitude;
    }

    public Sample getSample(int index) {
        return m_samples.get(index);
    }

    /**
     * Divides data by number of samples
     *
     * @param data Sum value
     * @return Averaged value
     */
    public float average(float data) {
        if (getNumSamples() > 0)
            return (data / getNumSamples());

        return 0;
    }

    /**
     * Computes gravity factor, given the sum of X/Y/Z accelerations through the test
     *
     * @param xBar Average X-Accel throughout test
     * @param yBar Average Y-Accel throughout test
     * @param zBar Average Z-Accel throughout test
     * @return gFactor coefficient
     */
    public float computeGFactor(float xBar, float yBar, float zBar) {

        float gFactor = GRAVITY / vectorMagnitude(xBar, yBar, zBar);

        return gFactor;
    }

    /**
     * Returns X,Y,Z components scaled to gravity
     *
     * @param sumX Sum of all X Accelerations in test
     * @param sumY Sum of all Y Accelerations in test
     * @param sumZ Sum of all Z Accelerations in test
     * @return Array containing X,Y,Z, component values for gravity compensation
     */
    public float[] computeCorrectionFactors(float sumX, float sumY, float sumZ) {
        float xBar = average(sumX);
        float yBar = average(sumY);
        float zBar = average(sumZ);

        float gFactor = computeGFactor(xBar, yBar, zBar);

        float xFactor = gFactor * xBar;
        float yFactor = gFactor * yBar;
        float zFactor = gFactor * zBar;

        return new float[]{xFactor, yFactor, zFactor};
    }

    /**
     * Computes instability of a given sample, scaled to remove gravity
     *
     * @param s                 The sample
     * @param correctionFactors Array containing X,Y,Z values to be subtracted from sample's components
     *                          to scale it to gravity
     * @return Calibrated instability of the sample
     */
    public float getSampleInstability(Sample s, float[] correctionFactors) {
        float x = s.x_cal();
        float y = s.y_cal();
        float z = s.z_cal();

        float xScaled = x - correctionFactors[X];
        float yScaled = y - correctionFactors[Y];
        float zScaled = z - correctionFactors[Z];

        return vectorMagnitude(xScaled, yScaled, zScaled);
    }

    /**
     * Computes a mean_r value.
     * Iterates through the entire m_samples ArrayList.
     *
     * @return mean_r
     */
    public float get_mean_r() {
        float instability = 0;
        float correctionFactors[] = computeCorrectionFactors(xSum, ySum, zSum);

        //Sums instability from each sample
        for (Sample sample : m_samples)
            instability += getSampleInstability(sample, correctionFactors);

        return average(instability);
    }

    public float getxSum() {
        return xSum;
    }

    public float getySum() {
        return ySum;
    }

    public float getzSum() {
        return zSum;
    }

    public class Sample {
        private float m_t;

        private float m_x;
        private float m_y;
        private float m_z;

        private float m_x_calibrated;
        private float m_y_calibrated;
        private float m_z_calibrated;

        public Sample(float t, float x, float y, float z) {
            m_t = t;

            m_x = x;
            m_y = y;
            m_z = z;

            m_x_calibrated = calibrate(x, X);
            m_y_calibrated = calibrate(y, Y);
            m_z_calibrated = calibrate(z, Z);
        }


        /**
         * Calibrates data using corresponding axis calibration data
         */
        public float calibrate(float data, int axis) {
            float result = 0;
            int axisLocation = axis;
            float correctionFactor;

            //Uses negative axis data if needed (e.g. X constant is defined as 0, XNEG is defined as 3)
            if (data < 0)
                axisLocation += 3;

            correctionFactor = (float) (GRAVITY / m_calibrationValues.get(axisLocation));
            result = data * correctionFactor;
            return result;
        }

        public float t() {
            return m_t;
        }

        public float x_cal() {
            return m_x_calibrated;
        }

        public float y_cal() {
            return m_y_calibrated;
        }

        public float z_cal() {
            return m_z_calibrated;
        }

        public float getM_x() {
            return m_x;
        }

        public float getM_y() {
            return m_y;
        }

        public float getM_z() {
            return m_z;
        }
    }
}
