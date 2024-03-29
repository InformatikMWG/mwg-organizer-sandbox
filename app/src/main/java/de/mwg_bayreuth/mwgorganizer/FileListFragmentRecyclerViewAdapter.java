package de.mwg_bayreuth.mwgorganizer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.mwg_bayreuth.mwgorganizer.FileListFragment.OnListFragmentInteractionListener;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link FileSelectionListContent.Item} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 *
 */
public class FileListFragmentRecyclerViewAdapter extends RecyclerView.Adapter<FileListFragmentRecyclerViewAdapter.ViewHolder> {

    private List<FileSelectionListContent.Item> mValues;
    private ViewHolder mViewHolder;
    private final OnListFragmentInteractionListener mListener;

    public FileListFragmentRecyclerViewAdapter(List<FileSelectionListContent.Item> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;

    }

    public void setItems(List<FileSelectionListContent.Item> items)
    {
        mValues = items;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_filelist_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        //holder.mIdView.setText(mValues.get(position).id);
        holder.mContentView.setText(mValues.get(position).content);
        if(!holder.mItem.updated) holder.mUpToDateButton.setVisibility(View.INVISIBLE);
        else                      holder.mUpToDateButton.setVisibility(View.VISIBLE);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        //public final TextView mIdView;
        public final TextView mContentView;
        public final ImageView mUpToDateButton;
        public FileSelectionListContent.Item mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            //mIdView = (TextView) view.findViewById(R.id.id);
            mContentView = (TextView) view.findViewById(R.id.content);
            mUpToDateButton = (ImageView) view.findViewById(R.id.upToDate);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
