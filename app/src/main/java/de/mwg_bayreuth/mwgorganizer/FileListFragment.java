package de.mwg_bayreuth.mwgorganizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
    private static final String ARG_FILESET = "FILESET";
    
    private String fileset;
    private Context mContext = null;
    private RecyclerView recyclerView = null;
    private FileSelectionListContent fileSelectionListContent = null;
    private OnListFragmentInteractionListener mListener;
    private FileListFragmentRecyclerViewAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FileListFragment() {
        super();
        fileSelectionListContent = new FileSelectionListContent();
    }

    // Parameter initialization
    public static FileListFragment newInstance(String fileset) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILESET, fileset);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            fileset = getArguments().getString(ARG_FILESET);
        }
    }

    public void onResume() {
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
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
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
                        for(int i = 0; i < pdffiles.length; i++) {
                            filenames[i] = pdffiles[i][1];
                            filelabels[i] = pdffiles[i][0];
                        }

                        Intent intent = new Intent(getActivity(), DisplayPDF.class);
                        intent.putExtra("FILESET", fileset);
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
    public void removeItem(int position) {
        fileSelectionListContent.removeItem(position);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Adding an item at the back of the List
     * @param item      Item to add to the List
     */
    public void addItem(FileSelectionListContent.Item item) {
        fileSelectionListContent.addItem(item);
        if(mAdapter != null)
        mAdapter.notifyDataSetChanged();
    }


    /**
     * Set up the single buttons
     * Please do *not* split up SharedPrefKeys like vplanFileNr into spRoot + variable + "string",
     * since the exact location of SP keys should be easily changed later in the SharedPrefKeys class
     */
    @Override
    void setupButtons(Context context) {
        fileSelectionListContent = new FileSelectionListContent();
        SharedPreferences sharedPref = context.getSharedPreferences(SharedPrefKeys.spRoot, Context.MODE_PRIVATE);

        int nrButtons;
        fileset = getArguments().getString(ARG_FILESET);
        if(fileset != null) {
            switch(fileset) {
                case ".vertplan":
                    nrButtons = sharedPref.getInt(SharedPrefKeys.vplanFileNr,0);
                    for(int i = 0; i < nrButtons; i++) {
                        addItem(new FileSelectionListContent.Item(""+i,
                                sharedPref.getString(SharedPrefKeys.vplanFileLabel+i, "NULL"),
                                sharedPref.getString(SharedPrefKeys.vplanFileFilename+i, "NULL"),
                                sharedPref.getBoolean(SharedPrefKeys.vplanFileUpdated+i, false)));
                    }
                    break;
                case ".mensa":
                    nrButtons = sharedPref.getInt(SharedPrefKeys.mensaFileNr,0);
                    for(int i = 0; i < nrButtons; i++) {
                        addItem(new FileSelectionListContent.Item(""+i,
                                sharedPref.getString(SharedPrefKeys.mensaFileLabel+i, "NULL"),
                                sharedPref.getString(SharedPrefKeys.mensaFileFilename+i, "NULL"),
                                sharedPref.getBoolean(SharedPrefKeys.mensaFileUpdated+i, false)));
                    }
                    break;
                default:
                    break;
            }
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

    public interface ClickListener{
        void onClick(View view,int position);
        void onLongClick(View view,int position);
    }

    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener{
        private ClickListener clicklistener;
        private GestureDetector gestureDetector;

        RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener){
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
        public void onTouchEvent(RecyclerView rv, MotionEvent e) { }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
    }
}
