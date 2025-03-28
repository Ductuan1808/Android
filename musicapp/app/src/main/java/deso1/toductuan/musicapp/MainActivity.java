package deso1.toductuan.musicapp;

import android.Manifest;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

    private MediaPlayer player;
    private ArrayList<SongEntity> listSong = new ArrayList<>();
    private TextView tvName, tvAlbum, tvTime;
    private SeekBar seekBar;
    private ImageView ivPlay;
    private int index = 0;
    private SongEntity songEntity;
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

        adapter = new MusicAdapter(listSong, this);
        rvSongs.setAdapter(adapter);

        checkPermissionAndLoadSongs();
    }

    private void checkPermissionAndLoadSongs() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            loadSongList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongList();
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền truy cập bộ nhớ để phát nhạc", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadSongList() {
        Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.TITLE + " ASC");
        if (c != null && c.moveToFirst()) {
            listSong.clear();
            do {
                String name = getColumnString(c, MediaStore.Audio.Media.TITLE);
                String path = getColumnString(c, MediaStore.Audio.Media.DATA);
                String album = getColumnString(c, MediaStore.Audio.Media.ALBUM);

                if (path != null && path.endsWith(".mp3")) {
                    listSong.add(new SongEntity(name, path, album));
                }
            } while (c.moveToNext());
            c.close();
        }

        adapter.notifyDataSetChanged();

        if (listSong.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy bài hát nào!", Toast.LENGTH_SHORT).show();
        } else {
            playSong(listSong.get(0));
        }
    }

    private String getColumnString(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "Unknown";
    }

    public void playSong(SongEntity song) {
        if (listSong.isEmpty() || song == null) return;

        index = listSong.indexOf(song);
        songEntity = song;

        tvName.setText(songEntity.getName());
        tvAlbum.setText(songEntity.getAlbum());

        if (player != null) {
            player.stop();
            player.release();
        }

        player = new MediaPlayer();
        try {
            player.setDataSource(songEntity.getPath());
            player.prepareAsync();
            player.setOnPreparedListener(mp -> {
                mp.start();
                ivPlay.setImageLevel(LEVEL_PLAY);
                totalTime = formatTime(mp.getDuration());
                seekBar.setMax(mp.getDuration());
                updateSeekBar();
            });

            player.setOnCompletionListener(mp -> nextSong());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi phát nhạc!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSeekBar() {
        new Thread(() -> {
            while (player != null && player.isPlaying()) {
                try {
                    Thread.sleep(500);
                    runOnUiThread(() -> {
                        if (player != null) {
                            seekBar.setProgress(player.getCurrentPosition());
                            tvTime.setText(String.format("%s / %s", formatTime(player.getCurrentPosition()), totalTime));
                        }
                    });
                } catch (Exception e) {
                    return;
                }
            }
        }).start();
    }

    private String formatTime(int time) {
        return new SimpleDateFormat("mm:ss").format(new Date(time));
    }

    public void playPause() {
        if (player == null) return;

        if (player.isPlaying()) {
            player.pause();
            ivPlay.setImageLevel(LEVEL_PAUSE);
        } else {
            player.start();
            ivPlay.setImageLevel(LEVEL_PLAY);
            updateSeekBar();
        }
    }

    private void nextSong() {
        if (listSong.isEmpty()) return;
        index = (index + 1) % listSong.size();
        playSong(listSong.get(index));
    }

    private void previousSong() {
        if (listSong.isEmpty()) return;
        index = (index - 1 + listSong.size()) % listSong.size();
        playSong(listSong.get(index));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_play) {
            playPause();
        } else if (v.getId() == R.id.iv_next) {
            nextSong();
        } else if (v.getId() == R.id.iv_back) {
            previousSong();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && player != null) {
            player.seekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
