package de.mwg_bayreuth.mwgorganizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class VertretungsplanFragment extends FileSelectionFragment {
    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private Context mContext = null;
    private RecyclerView recyclerView = null;
    private FileSelectionListContent fileSelectionListContent = null;
    private OnListFragmentInteractionListener mListener;
    private MyVertretungsplanRecyclerViewAdapter mAdapter;
    private SharedPreferences sharedPref;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public VertretungsplanFragment() {
        super();
        fileSelectionListContent = new FileSelectionListContent();
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static VertretungsplanFragment newInstance(int columnCount) {
        VertretungsplanFragment fragment = new VertretungsplanFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vertretungsplan_list, container, false);
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            mAdapter = new MyVertretungsplanRecyclerViewAdapter(fileSelectionListContent.ITEMS, mListener);
            recyclerView.setAdapter(mAdapter);
            recyclerView.addOnItemTouchListener(new RecyclerTouchListener(mContext,
                    recyclerView, new ClickListener() {

                @Override
                public void onClick(View view, final int position) {
                    if(fileSelectionListContent != null && position < fileSelectionListContent.ITEMS.size()) {
                        //The pdffilesArray is returned
                        //int position is the value of the chosen pdffile
                        //int currentfile = position
                        String[][] pdffiles = fileSelectionListContent.openPDF();

                    }
                }

                @Override
                public void onLongClick(View view, int position) {

                }

            }));
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Removing one item from the List
     * @param position  Position of the item that has to be removed
     */
    public void removeItem(int position)
    {
        fileSelectionListContent.removeItem(position);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Adding an item at the back of the List
     * @param item      Item to add to the List
     */
    public void addItem(FileSelectionListContent.Item item)
    {
        fileSelectionListContent.addItem(item);
        if(mAdapter != null)
        mAdapter.notifyDataSetChanged();
    }


    @Override
    void setupButtons(Context context) {
        fileSelectionListContent = new FileSelectionListContent();
        sharedPref = context.getSharedPreferences(SharedPrefKeys.spPrefix, Context.MODE_PRIVATE);
        int nrButtons = sharedPref.getInt(SharedPrefKeys.vplanButtonNr,0);
        for(int i = 0; i < nrButtons; i++) {
            addItem(new FileSelectionListContent.Item(""+i,
                    sharedPref.getString(SharedPrefKeys.vplanButtonLabel+i, "NULL"),
                    sharedPref.getString(SharedPrefKeys.vplanButtonFilename+i, "NULL"),
                    sharedPref.getBoolean(SharedPrefKeys.vplanButtonFileUpdated+i, false)));
        }
    }

    @Override
    void setLastUpdateTimeLabel() {

    }



    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(FileSelectionListContent.Item item);
    }

    public static interface ClickListener{
        public void onClick(View view,int position);
        public void onLongClick(View view,int position);
    }

    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{
        private ClickListener clicklistener;
        private GestureDetector gestureDetector;

        public RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener){

            this.clicklistener=clicklistener;
            gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child=recycleView.findChildViewUnder(e.getX(),e.getY());
                    if(child!=null && clicklistener!=null){
                        clicklistener.onLongClick(child,recycleView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child=rv.findChildViewUnder(e.getX(),e.getY());
            if(child!=null && clicklistener!=null && gestureDetector.onTouchEvent(e)){
                clicklistener.onClick(child,rv.getChildAdapterPosition(child));
            }

            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
}
