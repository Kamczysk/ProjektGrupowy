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

    // NOWOŚĆ: Zmienne do "progu czułości" (Histerezy)
    private double valleyValue = 999;
    // Minimalna zmiana jasności, żeby uznać to za uderzenie serca (można to później dostrajać)
    private static final double MIN_AMPLITUDE = 0.5;

    private static final long MIN_TIME_BETWEEN_BEATS_MS = 400;
    private long lastBeatTime = 0;

    private final java.util.ArrayList<Integer> bpmHistory = new java.util.ArrayList<>();
    private static final int HISTORY_SIZE = 9;

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

            // ZMIANA 1: Powiększamy obszar analizy, żeby złapać pulsujące "krawędzie" plamy światła
            int boxSize = 250;
            int startX = (width - boxSize) / 2;
            int startY = (height - boxSize) / 2;

            long totalBrightness = 0;
            int pixelCount = 0;

            for (int y = startY; y < startY + boxSize; y++) {
                for (int x = startX; x < startX + boxSize; x++) {
                    int index = y * width + x;
                    if (index < data.length) {
                        totalBrightness += (data[index] & 0xFF);
                        pixelCount++;
                    }
                }
            }

            if (pixelCount == 0) {
                image.close();
                return;
            }

            double currentRawValue = (double) totalBrightness / pixelCount;

            if (currentRawValue < 50) {
                bpmHistory.clear();
                image.close();
                return;
            }

            double currentFiltered = (currentRawValue + lastFilteredValue) / 2;

            // ZMIANA 2: Inteligentny próg czułości (odporność na małe drgania)
            if (currentFiltered < lastFilteredValue) {
                isDropping = true;
                // Zapisujemy najniższy punkt dołka
                if (currentFiltered < valleyValue) {
                    valleyValue = currentFiltered;
                }
            }
            else if (currentFiltered > lastFilteredValue && isDropping) {
                // Obraz zaczął jaśnieć. Czy to uderzenie, czy tylko szum?
                // Sprawdzamy, czy odbił się od "dna" (valleyValue) o co najmniej MIN_AMPLITUDE
                if (currentFiltered - valleyValue >= MIN_AMPLITUDE) {

                    isDropping = false; // Potwierdzamy uderzenie
                    valleyValue = 999;  // Resetujemy dołek dla następnego uderzenia

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