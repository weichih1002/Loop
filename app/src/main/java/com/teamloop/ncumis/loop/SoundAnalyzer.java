package com.teamloop.ncumis.loop;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.LinkedList;
import java.util.Observable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class SoundAnalyzer extends Observable implements AudioRecord.OnRecordPositionUpdateListener {
    public static final String TAG = "SoundAnalyzer";

    private static final int AUDIO_SAMPLING_RATE = 44100;
    private static int audioDataSize = 7200; // Length of sample to analyze.

    // Additive increase, very-additive decrease.
    private static double notifyRateinS = 0.15; // should be 2
    private static double minNotifyRate = 0.4; // at least every 0.4 s.
    private static double maxNotifyRate =
            (double)audioDataSize/(double)AUDIO_SAMPLING_RATE; // This corresponds to
    // real time analyze.

    // Enough to pick up most of the frequencies.
    private static final double MPM = 0.7;
    private static final double maxStDevOfMeanFrequency = 2.0; // if stdev bigger than that
    private static final double MaxPossibleFrequency = 2700.0;     // result considered rubbish
    private static final double loudnessThreshold = 30.0; // below is too quiet
    private static final double PercentOfWaveLengthSamplesToBeIgnored = 0.2;


    private AudioRecord audioRecord;
    private int bufferSize;
    private final CircularBuffer audioData;
    private short [] audioDataTemp;

    private Lock analyzingData;
    private double [] audioDataAnalysis; // because java's f*caked up
    DoubleFFT_1D fft_method;
    private int wavelengths;
    private double [] wavelength;
    private int elementsRead = 0;


    LinkedList noteList = new LinkedList();


    private boolean shouldAudioReaderThreadDie;
    Thread audioReaderThread;

    public static class AnalyzedSound {
        public enum ReadingType  {
            NO_PROBLEMS,
            TOO_QUIET,
            ZERO_SAMPLES,
            BIG_VARIANCE,
            BIG_FREQUENCY
        };
        public double loudness;
        public boolean frequencyAvailable;
        public double frequency;
        public ReadingType error;
        public AnalyzedSound(double l,ReadingType e) {
            loudness = l;
            frequencyAvailable = false;
            error = e;
        }
        public AnalyzedSound(double l, double f) {
            loudness = l;
            frequencyAvailable = true;
            frequency = f;
            error = ReadingType.NO_PROBLEMS;
        }
        public void getDebug() {
            if(error==ReadingType.NO_PROBLEMS)
                Log.d(TAG, "OK(" + frequency + ")");
            else if(error==ReadingType.ZERO_SAMPLES)
                Log.d(TAG,"Zero Samples (no wavelength established).");
            else if(error==ReadingType.BIG_VARIANCE)
                Log.d(TAG,"Variance on wavelengh too big.");
            else if(error==ReadingType.TOO_QUIET)
                Log.d(TAG,"Sound too quiet");
            else if(error==ReadingType.BIG_FREQUENCY)
                Log.d(TAG,"Frequency bigger than " + MaxPossibleFrequency);
            else  Log.e(TAG, "WTF - unknown AnalyzedSound message");
        }

    }   // end of class AnalyzedSound

    private void onNotifyRateChanged() {
        if(Math.random() < ConfigFlags.howOftenLogNotifyRate)
            Log.d(TAG, "Notify rate: " + notifyRateinS);
        if(audioRecord.setPositionNotificationPeriod(
                (int)(notifyRateinS*AUDIO_SAMPLING_RATE)) !=
                AudioRecord.SUCCESS) {
            Log.e(TAG, "Invalid notify rate.");
        }
    }

    // constructor of class SoundAnalyzer
    public SoundAnalyzer() throws Exception {
        // Setting up AudioRecord class.
        bufferSize =
                AudioRecord.getMinBufferSize(44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT)
                        * 2;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                AUDIO_SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        // Will notify us each time each notifyRateinThenthsOfS / 10 s.
        // However, we should still continuously read data, otherwise it wouldn't
        // work (I don't get why they did it).

        audioRecord.setRecordPositionUpdateListener(this);

        if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new Exception("Could not initialize microphone.");
        }
        onNotifyRateChanged();
        audioDataTemp = new short[audioDataSize];
        audioDataAnalysis = new double[4 * audioDataSize + 100];
        wavelength = new double[audioDataSize];
        audioData = new CircularBuffer(audioDataSize);
        analyzingData = new ReentrantLock();
        fft_method = new DoubleFFT_1D(audioDataSize);
    }

    public Long initTime;
    public void start() { // onStart
        audioRecord.startRecording();
        startAudioReaderThread();

        initTime = System.currentTimeMillis();
    }


    private void startAudioReaderThread() {
        shouldAudioReaderThreadDie = false;
        audioReaderThread = new Thread(new Runnable() {
            @Override

            public void run() {
                while(!shouldAudioReaderThreadDie) {
                    int shortsRead = audioRecord.read(audioDataTemp,0,audioDataSize);
                    if(shortsRead < 0) {
                        Log.e(TAG, "Could not read audio data.");
                    } else {
                        for(int i=0; i<shortsRead; ++i) {
                            audioData.push(audioDataTemp[i]);
                        }
                    }
                }
                Log.d(TAG, "AudioReaderThread reached the end");
            }
        });
        audioReaderThread.setDaemon(false);
        audioReaderThread.start();
    }

    private void stopAudioReaderThread() {
        shouldAudioReaderThreadDie = true;
        try {
            audioReaderThread.join();
        } catch(Exception e) {
            Log.e(TAG, "Could not join audioReaderThread: " + e.getMessage());
        }
    }

    public void ensureStarted() { // onResume
        Log.d(TAG, "Ensuring recording is on...");
        if(audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            Log.d(TAG, "I was worth ensuring recording is on.");
            audioRecord.startRecording();
        }
        if(audioReaderThread == null) {
            startAudioReaderThread();
        } else if(!audioReaderThread.isAlive()) {
            startAudioReaderThread();
        }

    }

    public void stop() { // onStop
        stopAudioReaderThread();
        audioRecord.stop();
        Log.d(TAG,"Analyzer Stop.");
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
        Log.e(TAG, "This should never happen - check AudioRecorded set up (notifications).");
        // This should never happen.
    }

    private AnalyzedSound analyzisResult;


    boolean dumpAudioData = false;

    public void dumpAudioDataRequest() {
        dumpAudioData = true;
    }

    public class ArrayToDump {
        public double [] arr;
        int elements;
        public ArrayToDump(double [] a, int e) {
            arr = a;
            elements = e;
        }
    }

    private void dumpAudioDataExecute() {
        setChanged();
        notifyObservers(new ArrayToDump(audioDataAnalysis, elementsRead));
    }

    // This is the periodic notification of AudioRecord listener.
    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        notifyObservers(analyzisResult);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!analyzingData.tryLock()) {
                    notifyRateinS=Math.min(notifyRateinS + 0.01, minNotifyRate);
                    onNotifyRateChanged();
                    if(ConfigFlags.shouldLogAnalyzisTooSlow)
                        Log.d(TAG, "Analyzing algorithm is too slow. Dropping sample");
                    return;
                } else {
                    notifyRateinS=Math.max(notifyRateinS - 0.001, maxNotifyRate);
                    onNotifyRateChanged();
                }
                analyzisResult = getFrequency();
                // Make sure we dump audioDataAfter analyzis.
                if(dumpAudioData) {
                    dumpAudioDataExecute();
                    dumpAudioData = false;
                    Log.d(TAG, "finished dumping.");
                }
                analyzingData.unlock();
                setChanged();
                // Log.e(TAG,"notified");
            }
        }).start();
    }

    // square.
    private double sq(double a) { return a*a; }

    private int currentFftMethodSize = -1;


    private double hanning(int n, int N) {
        return 0.5*(1.0 -Math.cos(2*Math.PI*(double)n/(double)(N-1)));
    }

    private void computeAutocorrelation() {
        // Fourier Transform to calculate autocorrelation. This kind of magic,
        // but it works :) Also some stupid people confuse definition of forward
        // transforms with inverse transforms. But the same people write multi-
        // threaded apps for computing them, so we kind of like them :)

        // Below i use circular convolution theorem.

        // Should save some memory.
        if(2*elementsRead != currentFftMethodSize) {
            fft_method = new DoubleFFT_1D(2*elementsRead);
            currentFftMethodSize = 2*elementsRead;
        }

        // Check out memory layout of fft methods in Jtransforms.
        for(int i=elementsRead-1; i>=0; i--) {
            audioDataAnalysis[2*i]=audioDataAnalysis[i] * hanning(i,elementsRead);
            audioDataAnalysis[2*i+1] = 0;
        }
        for(int i=2*elementsRead; i<audioDataAnalysis.length; ++i)
            audioDataAnalysis[i]=0;

        // Compute FORWARD fft transform.
        fft_method.complexInverse(audioDataAnalysis, false);

        // Replace every frequency with it's magnitude.
        for(int i=0; i<elementsRead; ++i) {
            audioDataAnalysis[2*i] = sq(audioDataAnalysis[2*i]) + sq(audioDataAnalysis[2*i+1]);
            audioDataAnalysis[2*i+1] = 0;
        }
        for(int i=2*elementsRead; i<audioDataAnalysis.length; ++i)
            audioDataAnalysis[i]=0;

        // Set first one on to 0.
        audioDataAnalysis[0] = 0;

        // Compute INVERSE fft.
        fft_method.complexForward(audioDataAnalysis);

        // Take real part of the result.
        for(int i=0; i<elementsRead; ++i)
            audioDataAnalysis[i] = audioDataAnalysis[2*i];
        for(int i=elementsRead; i<audioDataAnalysis.length; ++i)
            audioDataAnalysis[i]=0;
    }

    // Maybe will use it at some point
    void shoothOutAudioData() {
        double averageDistance = 0.0;
        for(int i=1; i<elementsRead; ++i)
            averageDistance += Math.abs(audioDataAnalysis[i]- audioDataAnalysis[i-1]);
        averageDistance/=elementsRead-1;

        int lastGoodPosition = 0;
        for(int i=1; i<elementsRead; ++i) {
            if(Math.abs(audioDataAnalysis[i]- audioDataAnalysis[lastGoodPosition]) <=
                    2*averageDistance) {
                audioDataAnalysis[++lastGoodPosition] = audioDataAnalysis[i];
            }
        }
        elementsRead = lastGoodPosition+1;
    }

    double getMeanWavelength() {
        double mean = 0;
        for(int i=0; i < wavelengths; ++i)
            mean += wavelength[i];
        mean/=(double)(wavelengths);
        return mean;
    }

    double getStDevOnWavelength() {
        double variance = 0; double mean = getMeanWavelength();
        for(int i=1; i < wavelengths; ++i)
            variance+= Math.pow(wavelength[i]-mean,2);
        variance/=(double)(wavelengths-1);
        return Math.sqrt(variance);
    }

    public void removeFalseSamples() {
        int samplesToBeIgnored =
                (int)(PercentOfWaveLengthSamplesToBeIgnored*wavelengths);
        if(wavelengths <=2) return;
        do {
            double mean = getMeanWavelength();
            // Looking for sample furthest away from mean.
            int best = -1;
            for(int i=0; i<wavelengths; ++i)
                if(best == -1 || Math.abs((double)wavelength[i]-mean) >
                        Math.abs((double)wavelength[best]-mean)) best = i;
            // Removing it.
            wavelength[best]=wavelength[wavelengths-1];
            --wavelengths;
        } while(getStDevOnWavelength() > maxStDevOfMeanFrequency &&
                samplesToBeIgnored-- > 0 && wavelengths > 2);
    }

    public AnalyzedSound getFrequency() {

        boolean haveSound = false;

        elementsRead =
                audioData.getElements(audioDataAnalysis,0,audioDataSize);
        double loudness = 0.0;
        for(int i=0; i<elementsRead; ++i)
            loudness+=Math.abs(audioDataAnalysis[i]);
        loudness/=elementsRead;
        // Check loudness first - it's root of all evil.
        if(loudness<loudnessThreshold)
            return new AnalyzedSound(loudness, AnalyzedSound.ReadingType.TOO_QUIET);


        computeAutocorrelation();

        double maximum=0;
        for(int i=1; i<elementsRead; ++i)
            maximum = Math.max(audioDataAnalysis[i], maximum);

        int lastStart = -1;
        wavelengths = 0;
        boolean passedZero = true;
        for(int i=0; i<elementsRead; ++i) {
            if(audioDataAnalysis[i]*audioDataAnalysis[i+1] <=0) passedZero = true;
            if(passedZero && audioDataAnalysis[i] > MPM*maximum &&
                    audioDataAnalysis[i] > audioDataAnalysis[i+1]) {
                if(lastStart != -1)
                    wavelength[wavelengths++]=i-lastStart;
                lastStart=i; passedZero = false;
                maximum = audioDataAnalysis[i];
            }
        }
        if(wavelengths < 2)      // 若收不到音，視為休止符。
        {
            /*long nowTime = System.currentTimeMillis();
            long deltaTime = nowTime - initTime;
            String timeMessage = ""+deltaTime;
            Log.d(TAG, "quiet recording,  time = " + timeMessage);
            initTime = nowTime;*/

            noteList.add("rest");

            return new AnalyzedSound(loudness, AnalyzedSound.ReadingType.ZERO_SAMPLES);
        }


        removeFalseSamples();

        double mean = getMeanWavelength(), stdv=getStDevOnWavelength();

        // 單音頻率 = 採樣頻率除以平均波長
        double calculatedFrequency = (double)AUDIO_SAMPLING_RATE/mean;

        // Log.d(TAG, "MEAN: " + mean + " STDV: " + stdv);
        // Log.d(TAG, "Frequency:" + calculatedFrequency);

        setFrequencyMessage(calculatedFrequency);

        if(stdv >= maxStDevOfMeanFrequency)
            return new AnalyzedSound(loudness, AnalyzedSound.ReadingType.BIG_VARIANCE);
        else if(calculatedFrequency>MaxPossibleFrequency)
            return new AnalyzedSound(loudness, AnalyzedSound.ReadingType.BIG_FREQUENCY);
        else
            return new AnalyzedSound(loudness, calculatedFrequency);

    }   // end method getFrequency


    public void setFrequencyMessage(double frequencyMessage)
    {
        /*DecimalFormat df = new DecimalFormat("##.00");
        frequencyMessage = Double.parseDouble(df.format(frequencyMessage));

        long nowTime = System.currentTimeMillis();
        long deltaTime = nowTime - initTime;    // 毫秒

        String timeMessage = ""+(deltaTime);    // 轉換為秒數 (String型態)
        initTime = nowTime;
        Log.d(TAG, "new recording node time : "+timeMessage);*/

        noteList.add(frequencyMessage);

    }

    public LinkedList getFrequencyMessage()
    {
        return noteList;
    }


}