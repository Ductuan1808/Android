package deso1.toductuan.musicapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import deso1.toductuan.musicapp.mymusic.MusicAdapter;
import deso1.toductuan.musicapp.mymusic.SongEntity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final int REQUEST_PERMISSION = 101;
    private static final int LEVEL_PAUSE = 0;
    private static final int LEVEL_PLAY = 1;
    private static final MediaPlayer player = new MediaPlayer();
    private static final int STATE_IDLE = 1;
    private static final int STATE_PLAYING = 2;
    private static final int STATE_PAUSED = 3;

    private final ArrayList<SongEntity> listSong = new ArrayList<>();
    private TextView tvName, tvAlbum, tvTime;
    private SeekBar seekBar;
    private ImageView ivPlay;
    private int index = 0;
    private SongEntity songEntity;
    private Thread thread;
    private int state = STATE_IDLE;
    private String totalTime;
    private RecyclerView rvSongs;
    private MusicAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        ivPlay = findViewById(R.id.iv_play);
        ivPlay.setOnClickListener(this);
        findViewById(R.id.iv_back).setOnClickListener(this);
        findViewById(R.id.iv_next).setOnClickListener(this);
        tvName = findViewById(R.id.tv_name);
        tvAlbum = findViewById(R.id.tv_album);
        tvTime = findViewById(R.id.tv_time);
        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        rvSongs = findViewById(R.id.rv_song);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo adapter trước khi load danh sách bài hát
        adapter = new MusicAdapter(listSong, this);
        rvSongs.setAdapter(adapter);

        // Kiểm tra và yêu cầu quyền truy cập dữ liệu media
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            loadingListSongOffline();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadingListSongOffline();
        } else {
            Toast.makeText(this, R.string.txt_alert, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadingListSongOffline() {
        Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            listSong.clear();
            do {
                String name = getColumnString(c, MediaStore.Audio.Media.TITLE);
                String path = getColumnString(c, MediaStore.Audio.Media.DATA);
                String album = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        ? getColumnString(c, MediaStore.Audio.Media.ALBUM_ARTIST)
                        : "N/A";

                listSong.add(new SongEntity(name, path, album));
            } while (c.moveToNext());
            c.close();
        }

        // Cập nhật dữ liệu mới cho adapter
        adapter.notifyDataSetChanged();

        if (listSong.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài hát nào!", Toast.LENGTH_SHORT).show();
        } else {
            play();
            playPause();
        }
    }

    private String getColumnString(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "N/A";
    }

    public void playSong(SongEntity songEntity) {
        index = listSong.indexOf(songEntity);
        this.songEntity = songEntity;
        play();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_play) {
            playPause();
        } else if (v.getId() == R.id.iv_next) {
            next();
        } else if (v.getId() == R.id.iv_back) {
            back();
        }
    }

    private void back() {
        index = (index == 0) ? listSong.size() - 1 : index - 1;
        play();
    }

    private void next() {
        index = (index >= listSong.size() - 1) ? 0 : index + 1;
        play();
    }

    private void playPause() {
        if (state == STATE_PLAYING && player.isPlaying()) {
            player.pause();
            ivPlay.setImageLevel(LEVEL_PAUSE);
            state = STATE_PAUSED;
        } else if (state == STATE_PAUSED) {
            player.start();
            state = STATE_PLAYING;
            ivPlay.setImageLevel(LEVEL_PLAY);
        } else {
            play();
        }
    }

    private void play() {
        if (listSong.isEmpty()) return;

        songEntity = listSong.get(index);
        tvName.setText(songEntity.getName());
        tvAlbum.setText(songEntity.getAlbum());
        player.reset();

        try {
            player.setDataSource(songEntity.getPath());
            player.prepare();
            player.start();
            ivPlay.setImageLevel(LEVEL_PLAY);
            state = STATE_PLAYING;
            totalTime = getTime(player.getDuration());
            seekBar.setMax(player.getDuration());

            if (thread == null) {
                startLooping();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startLooping() {
        thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    return;
                }
                runOnUiThread(this::updateTime);
            }
        });
        thread.start();
    }

    private void updateTime() {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            int time = player.getCurrentPosition();
            tvTime.setText(String.format("%s/%s", getTime(time), totalTime));
            seekBar.setProgress(time);
        }
    }

    @SuppressLint("SimpleDateFormat")
    private String getTime(int time) {
        return new SimpleDateFormat("mm:ss").format(new Date(time));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            player.seekTo(seekBar.getProgress());
        }
    }
}
