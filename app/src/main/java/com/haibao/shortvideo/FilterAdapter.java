package com.haibao.shortvideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wysaid.view.ImageGLSurfaceView;

/**
 * Created by why8222 on 2016/3/17.
 */
public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterHolder> {

    private Context context;
    private int selected = 0;

    public FilterAdapter(Context context) {
        this.context = context;
    }

    @Override
    public FilterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.filter_item_layout,
                parent, false);
        FilterHolder viewHolder = new FilterHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final FilterHolder holder, final int position) {
        final String _currentConfig=Contents.effectConfigs[position];
        final Bitmap _bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.filter_thumb_original);
        holder.thumbImage.setSurfaceCreatedCallback(new ImageGLSurfaceView.OnSurfaceCreatedCallback() {
            @Override
            public void surfaceCreated() {
                holder.thumbImage.setImageBitmap(_bitmap);
                holder.thumbImage.setFilterWithConfig(_currentConfig);
            }
        });
        if (position == selected) {
            holder.thumbSelected.setVisibility(View.VISIBLE);
            holder.thumbSelected_bg.setAlpha(0.7f);
        } else {
            holder.thumbSelected.setVisibility(View.GONE);
        }

        holder.filterRoot.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (selected == position)
                    return;
                int lastSelected = selected;
                selected = position;
                notifyItemChanged(lastSelected);
                notifyItemChanged(position);
                onFilterChangeListener.onFilterChanged(_currentConfig);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Contents.effectConfigs.length;
    }

    class FilterHolder extends RecyclerView.ViewHolder {
        ImageGLSurfaceView thumbImage;
        TextView filterName;
        FrameLayout thumbSelected;
        FrameLayout filterRoot;
        View thumbSelected_bg;

        public FilterHolder(View view) {
            super(view);
            thumbImage = (ImageGLSurfaceView) view
                    .findViewById(R.id.filter_thumb_image);
            filterName = (TextView) view
                    .findViewById(R.id.filter_thumb_name);
            filterRoot = (FrameLayout) view
                    .findViewById(R.id.filter_root);
            thumbSelected = (FrameLayout) view
                    .findViewById(R.id.filter_thumb_selected);
            thumbSelected_bg = view.
                    findViewById(R.id.filter_thumb_selected_bg);
        }
    }

    public interface onFilterChangeListener {
        void onFilterChanged(String filterType);
    }

    private onFilterChangeListener onFilterChangeListener;

    public void setOnFilterChangeListener(onFilterChangeListener onFilterChangeListener) {
        this.onFilterChangeListener = onFilterChangeListener;
    }
}
