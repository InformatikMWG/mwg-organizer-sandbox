package de.mwg_bayreuth.mwgorganizer;

import android.content.Context;
import android.content.Intent;
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
public class FileListFragment extends FileSelectionFragment {
    // TODO: Customize parameter argument names
    private static final String ARG_TYP = "TYPOFCLASS";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private String Typ;
    private Context mContext = null;
    private RecyclerView recyclerView = null;
    private FileSelectionListContent fileSelectionListContent = null;
    private OnListFragmentInteractionListener mListener;
    private FileListFragmentRecyclerViewAdapter mAdapter;
    private SharedPreferences sharedPref;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FileListFragment() {
        super();
        fileSelectionListContent = new FileSelectionListContent();
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static FileListFragment newInstance(String Typ) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYP, Typ);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            Typ = getArguments().getString(ARG_TYP);
        }
    }

    public void onResume()
    {
        super.onResume();
        setupButtons(mContext);
        mAdapter.setItems(fileSelectionListContent.ITEMS);
        recyclerView.setAdapter(null);
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filelist, container, false);
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            mAdapter = new FileListFragmentRecyclerViewAdapter(fileSelectionListContent.ITEMS, mListener);
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
                        String[] filenames = new String[pdffiles.length];
                        String[] filelabels = new String[pdffiles.length];
                        for(int i = 0; i < pdffiles.length; i++)
                        {
                            filenames[i] = pdffiles[i][1];
                            filelabels[i] = pdffiles[i][0];
                        }

                        Intent intent = new Intent(getActivity(), DisplayPDF.class);
                        intent.putExtra("CURRENTFILE", position);
                        intent.putExtra("NUMBEROFFILES", pdffiles.length);
                        intent.putExtra("PDFFILENAMES",filenames);
                        intent.putExtra("PDFFILELABELS",filelabels);
                        startActivity(intent);


                    }
                }

                @Override
                public void onLongClick(View view, int position) {
                    onClick(view, position);
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
        int nrButtons = sharedPref.getInt(SharedPrefKeys.spPrefix + Typ + ".buttons.number",0);
        for(int i = 0; i < nrButtons; i++) {
            addItem(new FileSelectionListContent.Item(""+i,
                    sharedPref.getString(SharedPrefKeys.spPrefix + Typ + ".buttons.label"+i, "NULL"),
                    sharedPref.getString(SharedPrefKeys.spPrefix + Typ + ".buttons.filename"+i, "NULL"),
                    sharedPref.getBoolean(SharedPrefKeys.spPrefix + Typ + ".buttons.fileupdated"+i, false)));
        }
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