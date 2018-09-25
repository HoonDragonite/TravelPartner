package kr.ac.shinhan.travelpartner.UI;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import kr.ac.shinhan.travelpartner.Adapter.AreaSpinnerAdapter;
import kr.ac.shinhan.travelpartner.Adapter.ContentTypeSpinnerAdapter;
import kr.ac.shinhan.travelpartner.Adapter.RecyclerAdapter;
import kr.ac.shinhan.travelpartner.Item.PlaceItem;
import kr.ac.shinhan.travelpartner.R;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.APPNAME;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.AREA_CODE;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.KEY;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.NUM_OF_ITEM;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.NUM_OF_ROWS;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.OS;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.SERVICE_AREACODE;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.SERVICE_AREA_BASED_LIST;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.SERVICE_SEARCH_KEYWORD;
import static kr.ac.shinhan.travelpartner.XMLparsing.ServiceDefinition.SERVICE_URL;

public class PlaceFragment extends Fragment {

    View view;
    private String guCode, contentType, arrange, contentId;
    private String title, tel, addr1, thumbnail;

    private int page;
    private Spinner mAreaSpinner, mContentTypeSpinner;
    private Button mSearchBtn;
    private FloatingActionButton mScrollBtn;
    private TextView mTitleArrange, mViewArrange; //버튼 셀렉터로 대체 가능
    private EditText mSearchEditText;
    private ArrayList<String> guNameList;
    private ArrayList<PlaceItem> items = new ArrayList<PlaceItem>();
    private HashMap<String, String> guCodeMap;
    private RecyclerAdapter recyclerAdapter = new RecyclerAdapter(this.getActivity(), items, R.layout.activity_main);

    private boolean mLoading = false;

    RecyclerView mRecyclerView;
    LinearLayoutManager mLayoutManager;

    public PlaceFragment(){

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_place, container, false);

        arrange = "O";
        contentType = "";
        guCode = "";
        page = 1;

        mSearchEditText = (EditText) view.findViewById(R.id.edittext_place_search);
        mSearchBtn = (Button) view.findViewById(R.id.btn_place_search);
        mSearchBtn.setOnClickListener(searchListener);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_place);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mTitleArrange = (TextView) view.findViewById(R.id.tv_place_arrange_title);
        mViewArrange = (TextView) view.findViewById(R.id.tv_place_arrange_view);
        mTitleArrange.setOnClickListener(sortListener);
        mViewArrange.setOnClickListener(sortListener);

        mScrollBtn = (FloatingActionButton) view.findViewById(R.id.btn_place_scrollup);
        mScrollBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecyclerView.smoothScrollToPosition(0);
            }
        });

        new AreaCode().execute();
        PlaceItemParsing placeItemParsing = new PlaceItemParsing();
        placeItemParsing.execute(guCode, contentType, arrange, Integer.toString(page));

        mRecyclerView.setAdapter(recyclerAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItem = mLayoutManager.getItemCount();
                int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();

                if (!mLoading && lastVisibleItem == totalItem - 1) {
                    mLoading = true;
                    page++;
                    new PlaceItemParsing().execute(guCode, contentType, arrange, Integer.toString(page));
                }
            }
        });
        mRecyclerView.setNestedScrollingEnabled(false);

        return view;
    }

    public void initSpinner() {
        String[] contentTypeList = {"전체 타입", "관광지", "문화시설", "레포츠", "숙박", "쇼핑", "음식점"};

        mAreaSpinner = view.findViewById(R.id.spinner_place_area);
        AreaSpinnerAdapter mAreaSpinnerAdapter = new AreaSpinnerAdapter(this.getActivity(), android.R.layout.simple_spinner_item, guNameList);
        mAreaSpinner.setAdapter(mAreaSpinnerAdapter);
        mAreaSpinner.setOnItemSelectedListener(areaSpinnerListener);

        mContentTypeSpinner = view.findViewById(R.id.spinner_place_contenttype);
        ContentTypeSpinnerAdapter mContentTypeAdapter = new ContentTypeSpinnerAdapter(this.getActivity(), android.R.layout.simple_spinner_item, contentTypeList);
        mContentTypeSpinner.setAdapter(mContentTypeAdapter);
        mContentTypeSpinner.setOnItemSelectedListener(contentTypeSpinnerListener);
    }

    View.OnClickListener searchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String keyword = mSearchEditText.getText().toString();
            new SerachKeyword().execute(keyword);
        }
    };

    View.OnClickListener sortListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_place_arrange_title:
                    arrange = "O";
                    new PlaceItemParsing().execute(guCode, contentType, arrange, Integer.toString(page));
                    break;
                case R.id.tv_place_arrange_view:
                    arrange = "P";
                    new PlaceItemParsing().execute(guCode, contentType, arrange, Integer.toString(page));
                    break;
            }
        }
    };

    AdapterView.OnItemSelectedListener areaSpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String guName = guNameList.get(position);
            guCode = guCodeMap.get(guName);
            Toast.makeText(getActivity(), "guCode : " + guCode, Toast.LENGTH_SHORT).show();
            new PlaceItemParsing().execute(guCode, contentType, arrange, Integer.toString(page));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    AdapterView.OnItemSelectedListener contentTypeSpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String[] contentTypeList = {"", "12", "14", "28", "32", "38", "39"};
            contentType = contentTypeList[position];
            Toast.makeText(getActivity(), "contentType : " + contentType, Toast.LENGTH_SHORT).show();
            new PlaceItemParsing().execute(guCode, contentType, arrange, Integer.toString(page));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    class PlaceItemParsing extends AsyncTask<String, String, ArrayList<PlaceItem>> {
        @Override
        protected ArrayList<PlaceItem> doInBackground(String... strings) {
            try {
                String guCode, contentType, arrange, page;
                guCode = strings[0];
                contentType = strings[1];
                arrange = strings[2];
                page = strings[3];
                items.clear();

                URL areaBasedListURL = new URL(SERVICE_URL + SERVICE_AREA_BASED_LIST + "ServiceKey=" + KEY + "&MobileOS=" + OS + "&MobileApp=" + APPNAME + "&areaCode=" + AREA_CODE
                        + "&numOfRows=" + NUM_OF_ITEM + "&pageNo=" + page + "&arrange=" + arrange + "&contentTypeId=" + contentType + "&sigunguCode=" + guCode);
                XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
                XmlPullParser parser = parserCreator.newPullParser();

                // 지역기반 관광 정보 조회 파싱
                parser.setInput(areaBasedListURL.openStream(), "UTF-8");
                int parserEvent = parser.getEventType();

                PlaceItem placeItem = null;
                while (parserEvent != XmlPullParser.END_DOCUMENT) {
                    switch (parserEvent) {
                        case XmlPullParser.START_TAG:
                            String tag = parser.getName();
                            if (tag.equals("item")) {
                                placeItem = new PlaceItem();
                            } else if (tag.equals("addr1")) {
                                parser.next();
                                addr1 = parser.getText();
                                placeItem.setAddr(addr1);
                            } else if (tag.equals("contentid")) {
                                parser.next();
                                contentId = parser.getText();
                                placeItem.setContentId(contentId);
                            } else if (tag.equals("firstimage2")) {
                                parser.next();
                                thumbnail = parser.getText();
                                placeItem.setImage(thumbnail);
                            } else if (tag.equals("tel")) {
                                parser.next();
                                tel = parser.getText();
                                placeItem.setTel(tel);
                            } else if (tag.equals("title")) {
                                parser.next();
                                title = parser.getText();
                                placeItem.setTitle(title);
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            String endTag = parser.getName();
                            if (endTag.equals("item")) {
                                items.add(placeItem);
                            }
                            break;
                    }
                    parserEvent = parser.next();
                }
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
            return items;
        }

        @Override
        protected void onPostExecute(ArrayList<PlaceItem> placeItems) {
            super.onPostExecute(placeItems);
            if (placeItems.isEmpty()) {
                //items.add(new PlaceItem());
            }
            mLoading = false;
            mRecyclerView.getRecycledViewPool().clear();
            //mLayoutManager.scrollToPositionWithOffset(0,0);
            recyclerAdapter.notifyDataSetChanged();
        }
    }

    class AreaCode extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                URL url = new URL(SERVICE_URL + SERVICE_AREACODE + "ServiceKey=" + KEY + "&MobileOS=" + OS + "&MobileApp=" + APPNAME + "&areaCode=" + AREA_CODE
                        + "&numOfRows=" + NUM_OF_ROWS);
                XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
                XmlPullParser parser = parserCreator.newPullParser();
                parser.setInput(url.openStream(), "UTF-8");
                int parserEvent = parser.getEventType();
                guCodeMap = new HashMap<String, String>();
                String key = "", value = "";
                while (parserEvent != XmlPullParser.END_DOCUMENT) {
                    switch (parserEvent) {
                        case XmlPullParser.START_TAG:
                            String tag = parser.getName();
                            if (tag.equals("code")) {
                                parser.next();
                                value = parser.getText();

                            } else if (tag.equals("name")) {
                                parser.next();
                                key = parser.getText();
                            }
                            if (!guCodeMap.containsKey(key))
                                guCodeMap.put(key, value);
                            break;
                    }
                    parserEvent = parser.next();
                    guNameList = new ArrayList<String>();
                    guCodeMap.remove("");
                    for (Map.Entry<String, String> entry : guCodeMap.entrySet()) {
                        guNameList.add(entry.getKey());
                    }
                }
                guCodeMap.put("서울 전체", "");
                Collections.sort(guNameList);
                guNameList.add(0, "서울 전체");
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            initSpinner();
        }
    }

    class SerachKeyword extends AsyncTask<String, String, ArrayList<PlaceItem>> {
        @Override
        protected ArrayList<PlaceItem> doInBackground(String... strings) {
            try {
                //keyword = URLEncoder.encode(strings[0], "UTF-8");
                String keyword = strings[0];

                items.clear();
                Log.d("hoon", "인코딩한 keyword : " + keyword);
                URL url = new URL(SERVICE_URL + SERVICE_SEARCH_KEYWORD + "ServiceKey=" + KEY + "&MobileOS=" + OS + "&MobileApp=" + APPNAME + "&areaCode=" + AREA_CODE
                        + "&keyword=" + URLEncoder.encode(keyword, "UTF-8"));
                Log.d("hoon", "URL" + url);
                XmlPullParserFactory parserCreator = XmlPullParserFactory.newInstance();
                XmlPullParser parser = parserCreator.newPullParser();
                parser.setInput(url.openStream(), "UTF-8");
                int parserEvent = parser.getEventType();

                PlaceItem placeItem = null;
                while (parserEvent != XmlPullParser.END_DOCUMENT) {
                    switch (parserEvent) {
                        case XmlPullParser.START_TAG:
                            String tag = parser.getName();
                            if (tag.equals("item")) {
                                placeItem = new PlaceItem();
                            } else if (tag.equals("addr1")) {
                                parser.next();
                                addr1 = parser.getText();
                                placeItem.setAddr(addr1);
                            } else if (tag.equals("contentid")) {
                                parser.next();
                                contentId = parser.getText();
                                placeItem.setContentId(contentId);
                            } else if (tag.equals("firstimage2")) {
                                parser.next();
                                thumbnail = parser.getText();
                                placeItem.setImage(thumbnail);
                            } else if (tag.equals("tel")) {
                                parser.next();
                                tel = parser.getText();
                                placeItem.setTel(tel);
                            } else if (tag.equals("title")) {
                                parser.next();
                                title = parser.getText();
                                placeItem.setTitle(title);
                                break;
                            }

                        case XmlPullParser.END_TAG:
                            String endTag = parser.getName();
                            if (endTag.equals("item")) {
                                items.add(placeItem);
                            }
                            break;
                    }
                    parserEvent = parser.next();
                }
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
            return items;
        }

        @Override
        protected void onPostExecute(ArrayList<PlaceItem> placeItems) {
            super.onPostExecute(placeItems);
            if (placeItems.isEmpty()) {
                //items.add(new PlaceItem());
            }
            recyclerAdapter.notifyDataSetChanged();
        }
    }
}
