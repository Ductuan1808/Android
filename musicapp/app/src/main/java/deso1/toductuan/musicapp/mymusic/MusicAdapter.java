package deso1.toductuan.musicapp.mymusic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import deso1.toductuan.musicapp.MainActivity;
import deso1.toductuan.musicapp.R;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicHolder> {
    private final ArrayList<SongEntity> listSong;
    private final MainActivity mainActivity;

    public MusicAdapter(ArrayList<SongEntity> listSong, MainActivity mainActivity) {
        this.listSong = listSong;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public MusicHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new MusicHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicHolder holder, int position) {
        if (position >= 0 && position < listSong.size()) { // Kiểm tra tránh lỗi
            SongEntity item = listSong.get(position);
            holder.tvName.setText(item.getName());
            holder.tvName.setTag(item);
        }
    }

    @Override
    public int getItemCount() {
        return listSong != null ? listSong.size() : 0;
    }

    public class MusicHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public MusicHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_song);
            itemView.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(itemView.getContext(), android.R.anim.fade_in)); // Sửa lỗi animation
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mainActivity.playSong(listSong.get(position)); // Gọi trực tiếp MainActivity
                }
            });
        }
    }
}
