package com.example.heartrateapp;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class Game2048Activity extends AppCompatActivity implements View.OnTouchListener {

    private static final int SIZE = 4;
    private static final int SWIPE_THRESHOLD_PX = 80;

    private final int[][] board = new int[SIZE][SIZE];
    private final TextView[][] cells = new TextView[SIZE][SIZE];
    private final Random random = new Random();

    private GridLayout grid;
    private TextView txtScore;
    private TextView txtHint;
    private int score = 0;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_2048);

        grid = findViewById(R.id.grid2048);
        txtScore = findViewById(R.id.txtScore2048);
        txtHint = findViewById(R.id.txtHint2048);
        Button btnRestart = findViewById(R.id.btnRestart2048);
        Button btnBack = findViewById(R.id.btnBack2048);

        setupGridUi();

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();

                if (Math.abs(dx) < SWIPE_THRESHOLD_PX && Math.abs(dy) < SWIPE_THRESHOLD_PX) {
                    return false;
                }

                boolean moved;
                if (Math.abs(dx) > Math.abs(dy)) {
                    moved = dx > 0 ? moveRight() : moveLeft();
                } else {
                    moved = dy > 0 ? moveDown() : moveUp();
                }

                if (moved) {
                    addRandomTile();
                    updateUi();
                    if (isGameOver()) {
                        Toast.makeText(Game2048Activity.this, "Koniec gry! Kliknij \"Nowa gra\".", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });

        grid.setOnTouchListener(this);

        btnRestart.setOnClickListener(v -> resetGame());
        btnBack.setOnClickListener(v -> finish());

        resetGame();
    }

    private void setupGridUi() {
        grid.setRowCount(SIZE);
        grid.setColumnCount(SIZE);

        int cellSizeDp = 72;
        int cellSizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                cellSizeDp,
                getResources().getDisplayMetrics()
        );

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                TextView tv = new TextView(this);
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                tv.setBackgroundResource(R.drawable.tile_bg);
                tv.setPadding(8, 8, 8, 8);

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                        GridLayout.spec(r, 1f),
                        GridLayout.spec(c, 1f)
                );
                lp.width = cellSizePx;
                lp.height = cellSizePx;
                lp.setMargins(8, 8, 8, 8);
                tv.setLayoutParams(lp);

                grid.addView(tv);
                cells[r][c] = tv;
            }
        }
    }

    private void resetGame() {
        score = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                board[r][c] = 0;
            }
        }
        addRandomTile();
        addRandomTile();
        updateUi();
        txtHint.setText("Przesuwaj palcem: lewo / prawo / góra / dół");
    }

    private void updateUi() {
        txtScore.setText("Wynik: " + score);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int v = board[r][c];
                TextView tv = cells[r][c];
                tv.setText(v == 0 ? "" : String.valueOf(v));
            }
        }
    }

    private void addRandomTile() {
        int emptyCount = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) emptyCount++;
            }
        }
        if (emptyCount == 0) return;

        int pick = random.nextInt(emptyCount);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) {
                    if (pick == 0) {
                        board[r][c] = random.nextFloat() < 0.9f ? 2 : 4;
                        return;
                    }
                    pick--;
                }
            }
        }
    }

    private boolean moveLeft() {
        boolean moved = false;
        for (int r = 0; r < SIZE; r++) {
            int[] line = new int[SIZE];
            for (int c = 0; c < SIZE; c++) line[c] = board[r][c];

            int[] merged = mergeLineLeft(line);
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != merged[c]) moved = true;
                board[r][c] = merged[c];
            }
        }
        return moved;
    }

    private boolean moveRight() {
        boolean moved = false;
        for (int r = 0; r < SIZE; r++) {
            int[] line = new int[SIZE];
            for (int c = 0; c < SIZE; c++) line[c] = board[r][c];

            reverse(line);
            int[] merged = mergeLineLeft(line);
            reverse(merged);

            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] != merged[c]) moved = true;
                board[r][c] = merged[c];
            }
        }
        return moved;
    }

    private boolean moveUp() {
        boolean moved = false;
        for (int c = 0; c < SIZE; c++) {
            int[] line = new int[SIZE];
            for (int r = 0; r < SIZE; r++) line[r] = board[r][c];

            int[] merged = mergeLineLeft(line);
            for (int r = 0; r < SIZE; r++) {
                if (board[r][c] != merged[r]) moved = true;
                board[r][c] = merged[r];
            }
        }
        return moved;
    }

    private boolean moveDown() {
        boolean moved = false;
        for (int c = 0; c < SIZE; c++) {
            int[] line = new int[SIZE];
            for (int r = 0; r < SIZE; r++) line[r] = board[r][c];

            reverse(line);
            int[] merged = mergeLineLeft(line);
            reverse(merged);

            for (int r = 0; r < SIZE; r++) {
                if (board[r][c] != merged[r]) moved = true;
                board[r][c] = merged[r];
            }
        }
        return moved;
    }

    private int[] mergeLineLeft(int[] line) {
        int[] res = new int[SIZE];
        int idx = 0;

        for (int v : line) if (v != 0) res[idx++] = v;

        for (int i = 0; i < SIZE - 1; i++) {
            if (res[i] != 0 && res[i] == res[i + 1]) {
                res[i] *= 2;
                score += res[i];
                res[i + 1] = 0;
                i++;
            }
        }

        int[] finalLine = new int[SIZE];
        idx = 0;
        for (int v : res) if (v != 0) finalLine[idx++] = v;
        return finalLine;
    }

    private void reverse(int[] arr) {
        for (int i = 0, j = arr.length - 1; i < j; i++, j--) {
            int t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
    }

    private boolean isGameOver() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) return false;
            }
        }

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int v = board[r][c];
                if (r + 1 < SIZE && board[r + 1][c] == v) return false;
                if (c + 1 < SIZE && board[r][c + 1] == v) return false;
            }
        }
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
}
