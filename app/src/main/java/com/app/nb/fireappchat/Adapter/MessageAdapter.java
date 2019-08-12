package com.app.nb.fireappchat.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.nb.fireappchat.Model.FireAppMessage;
import com.app.nb.fireappchat.R;
import com.bumptech.glide.Glide;

import java.util.List;

public class MessageAdapter extends BaseAdapter {

    private Context context;
    private int layout;
    private List<FireAppMessage> messages;

    public MessageAdapter(Context context, int layout, List<FireAppMessage> messages) {
        this.context = context;
        this.layout = layout;
        this.messages = messages;
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {

        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(layout, null);
            holder.ivPhoto = convertView.findViewById(R.id.ivPhoto);
            holder.tvMessage = convertView.findViewById(R.id.tvMessage);
            holder.tvName = convertView.findViewById(R.id.tvName);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FireAppMessage message = messages.get(position);

        boolean isPhoto = message.getPhotoUrl() != null;

        if (isPhoto) {
            holder.tvMessage.setVisibility(View.GONE);
            holder.ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(holder.ivPhoto.getContext())
                    .load(message.getPhotoUrl())
                    .into(holder.ivPhoto);
        } else {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.ivPhoto.setVisibility(View.GONE);
            holder.tvMessage.setText(message.getText());
        }

        holder.tvName.setText(message.getName());

        return convertView;

    }

    class ViewHolder {
        ImageView ivPhoto;
        TextView tvMessage;
        TextView tvName;
    }
}
