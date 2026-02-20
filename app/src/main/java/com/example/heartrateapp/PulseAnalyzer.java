package com.example.heartrateapp;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

public class PulseAnalyzer implements androidx.camera.core.ImageAnalysis.Analyzer {

    public interface PulseListener {
        void onPulseDetected(int bpm);
    }

    private final PulseListener listener;

    private double lastFilteredValue = 0;
    private boolean isDropping = false;

    private double valleyValue = 999;
    // ZMIANA: Zwiększony próg czułości na szum (ignoruje małe skoki)
    private static final double MIN_AMPLITUDE = 2.0;

    private static final long MIN_TIME_BETWEEN_BEATS_MS = 450;
    private long lastBeatTime = 0;

    private final java.util.ArrayList<Integer> bpmHistory = new java.util.ArrayList<>();
    private static final int HISTORY_SIZE = 9;

    // NOWOŚĆ: Bufor do mocniejszego wygładzania sygnału
    private final java.util.ArrayList<Double> smoothingBuffer = new java.util.ArrayList<>();
    private static final int SMOOTHING_WINDOW = 8; // Uśredniamy n ostatnich klatek

    private long startTime = 0;
    private static final long WARMUP_TIME_MS = 3000;

    public PulseAnalyzer(PulseListener listener) {
        this.listener = listener;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        long currentTime = System.currentTimeMillis();

        if (startTime == 0) {
            startTime = currentTime;
        }
        if (currentTime - startTime < WARMUP_TIME_MS) {
            image.close();
            return;
        }

        try {
            java.nio.ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            int width = image.getWidth();
            int height = image.getHeight();

            long totalBrightness = 0;
            int validPixelCount = 0;

            // ZMIANA: Skanujemy cały ekran, ale co 5 pikseli (dla szybkości)
            for (int y = 0; y < height; y += 5) {
                for (int x = 0; x < width; x += 5) {
                    int index = y * width + x;
                    if (index < data.length) {
                        int pixelValue = data[index] & 0xFF;

                        // ZMIANA: "Matematyczny Pierścień"
                        // Ignorujemy prześwietlony środek (> 245) i czarne krawędzie (< 50)
                        if (pixelValue > 50 && pixelValue < 245) {
                            totalBrightness += pixelValue;
                            validPixelCount++;
                        }
                    }
                }
            }

            // Jeśli cały ekran jest prześwietlony lub czarny, ignorujemy klatkę
            if (validPixelCount == 0) {
                image.close();
                return;
            }

            double currentRawValue = (double) totalBrightness / validPixelCount;

            // ZMIANA: Mocniejsze wygładzanie (Średnia Krocząca)
            smoothingBuffer.add(currentRawValue);
            if (smoothingBuffer.size() > SMOOTHING_WINDOW) {
                smoothingBuffer.remove(0);
            }

            double sum = 0;
            for (double val : smoothingBuffer) {
                sum += val;
            }
            double currentFiltered = sum / smoothingBuffer.size();

            // Reszta algorytmu (szukanie dołka i obliczanie BPM)
            if (currentFiltered < lastFilteredValue) {
                isDropping = true;
                if (currentFiltered < valleyValue) {
                    valleyValue = currentFiltered;
                }
            }
            else if (currentFiltered > lastFilteredValue && isDropping) {
                if (currentFiltered - valleyValue >= MIN_AMPLITUDE) {
                    isDropping = false;
                    valleyValue = 999;

                    long timeDifference = currentTime - lastBeatTime;

                    if (timeDifference > MIN_TIME_BETWEEN_BEATS_MS) {
                        double instantBpm = 60000.0 / timeDifference;

                        if (instantBpm > 45 && instantBpm < 170) {
                            bpmHistory.add((int) instantBpm);
                            if (bpmHistory.size() > HISTORY_SIZE) {
                                bpmHistory.remove(0);
                            }

                            java.util.ArrayList<Integer> sortedHistory = new java.util.ArrayList<>(bpmHistory);
                            java.util.Collections.sort(sortedHistory);
                            int medianBpm = sortedHistory.get(sortedHistory.size() / 2);

                            if (listener != null) {
                                listener.onPulseDetected(medianBpm);
                            }
                        }
                        lastBeatTime = currentTime;
                    }
                }
            }
            lastFilteredValue = currentFiltered;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            image.close();
        }
    }
}