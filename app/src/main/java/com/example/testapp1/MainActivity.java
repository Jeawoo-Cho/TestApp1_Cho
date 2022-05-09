package com.example.testapp1;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapView; //카카오 맵뷰 임포트

import android.util.Log;
import android.view.ViewGroup; //뷰 그룹 임포트
import android.widget.TextView;
import android.support.v7.widget.SearchView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapView mapView = new MapView(this);


        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view); //뷰그룹 사용
        mapViewContainer.addView(mapView);

        //트래킹 모드
        //mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);

        //Searchparser(테스트)
        SearchParser searchParser = new SearchParser();


        //Coordparse (테스트)
        CoordParser apiData = new CoordParser();


        //PolyLineSeter클래스
        PolylineSeter polylineSeter = new PolylineSeter();


        //텍스트뷰 (테스트)
        TextView textView1 = (TextView) findViewById(R.id.text1) ;

        //검색(SearchView) 테스트
        SearchView searchView = findViewById(R.id.SearchView1);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                ArrayList<SearchCoord> scData = searchParser.getCoord(s);
                ArrayList<CoorData> dataArr = apiData.getData(scData.get(0).SearchHelper());
                polylineSeter.set_poly(mapView, dataArr);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

    }

}



class SearchCoord {
    String x;
    String y;
    public String SearchHelper() {
        Double minX = Double.parseDouble(x)-0.01;
        Double maxX = Double.parseDouble(x)+0.01;
        Double minY = Double.parseDouble(y)-0.01;
        Double maxY = Double.parseDouble(y)+0.01;
        String result = minX.toString()+","+minY.toString()+","+maxX.toString()+","+maxY.toString()+")";
        return result;
    }
}

class SearchParser {
    public ArrayList<SearchCoord> getCoord(String mName) {
        //return data 부분
        ArrayList<SearchCoord> searchCoords = new ArrayList<SearchCoord>();
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    //요청 Url
                    String searchUrl = "https://api.vworld.kr/req/search?service=search&request=search&version=2.0&crs=EPSG:4326&size=10&page=1&type=place&format=xml&errorformat=xml&key=F931BD24-945F-3AA9-8CB7-853B5D40C5A8&query=" + mName;
                    URL url = new URL(searchUrl);
                    InputStream is = url.openStream();

                    //xmlParser 생성
                    XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = xmlFactory.newPullParser();
                    parser.setInput(is,"utf-8");

                    //xml과 관련된 변수들
                    boolean isTitle = false;
                    boolean isThat = false;
                    boolean isFirst = true;
                    boolean isCate = false;
                    boolean isMount = false;
                    boolean isX = false;
                    boolean isY = false;
                    String X = "";
                    String Y = "";
                    String Title = "";
                    String Cate = "";

                    // 파싱 시작
                    while(parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                        int type = parser.getEventType();
                        SearchCoord sc = new SearchCoord();

                        //태그 검사(태그가 gml:posList인 경우 찾기)
                        if(type == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("category")) {
                                isCate = true;
                            }
                            else if(parser.getName().equals("title")){
                                isTitle = true;
                            }
                            else if(parser.getName().equals("x")){
                                isX = true;
                            }
                            else if(parser.getName().equals("y")){
                                isY = true;
                            }
                        }

                        else if(type == XmlPullParser.TEXT) { //텍스트확인
                            if(isTitle){ //태그가 타이틀일 때
                                if(isFirst){
                                    Title = parser.getText();
                                }
                                if(parser.getText()==Title){
                                    isThat = true;
                                }
                                isTitle = false;
                            }
                            else if(isCate) { //태그가 카테고리일때
                                if(isFirst){
                                    Cate = parser.getText();
                                    isFirst = false;
                                }
                                if(parser.getText()==Cate){//데이터 분류가 산이라면
                                    isMount = true; //이 데이터가 산임을 명시
                                }
                                isCate = false; //카테고리 태그 끝
                            }
                            else if(isX) { //태그가 x일 때
                                if(isThat && isMount){ //데이터가 입력한 산이라면
                                    X = parser.getText(); //x좌표 X에 저장
                                }
                                isX = false; //x 태그 끝
                            }
                            else if(isY) { //태그가 y일 때
                                if(isThat && isMount) { //데이터가 입력한 산이라면
                                    Y = parser.getText(); // y좌표 Y에 저장
                                }
                                isY = false; //y 태그 끝
                            }
                        }

                        // y태그가 끝날 때데이터 추가 ()
                        else if(type == XmlPullParser.END_TAG && parser.getName().equals("y") && isThat && isMount) {
                            sc.x = X;
                            sc.y = Y;
                            searchCoords.add(sc);
                            isThat = false; // 산 데이터에 대한 x,y 좌표 저장 끝났으므로
                            isMount = false; //isThat과 isMount 초기화
                        }


                        type = parser.next();
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return searchCoords;
    }
}

class CoorData {
    String[] Coords;
}

class CoordParser {
    public ArrayList<CoorData> getData(String boxData) {
        //return data 부분
        ArrayList<CoorData> dataArr = new ArrayList<CoorData>();
        Thread t = new Thread() {
            @Override
            public  void run() {
                try {
                    //요청 Url
                    String fullurl = "https://api.vworld.kr/req/data?service=data&version=2.0&request=getfeature&key=F931BD24-945F-3AA9-8CB7-853B5D40C5A8&domain=http://localhost:8080&format=xml&data=LT_L_FRSTCLIMB&crs=epsg:4326&geomfilter=BOX("+boxData;
                    URL url = new URL(fullurl);
                    InputStream is = url.openStream();

                    //xmlParser 생성
                    XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = xmlFactory.newPullParser();
                    parser.setInput(is,"utf-8");

                    //xml과 관련된 변수들
                    boolean isCoords = false;
                    String Coords = "";

                    // 파싱 시작
                    while(parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                        int type = parser.getEventType();
                        CoorData data = new CoorData();

                        //태그 검사(태그가 gml:posList인 경우 찾기)
                        if(type == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("gml:posList")) {
                              isCoords = true;
                            }
                        }
                        //텍스트 확인 (Coords에 텍스트 임시 저장)
                        else if(type == XmlPullParser.TEXT) {
                            if(isCoords) {
                                Coords = parser.getText();
                                isCoords = false;
                            }
                        }
                        // 데이터 추가 (Coords데이터 공백으로 스플릿하여 저장)
                        else if(type == XmlPullParser.END_TAG && parser.getName().equals("gml:posList")) {
                            data.Coords = Coords.split(" ");

                            dataArr.add(data);
                        }

                        type = parser.next();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return dataArr;
    }
}

class PolylineSeter {
    public void set_poly(MapView mapView, ArrayList<CoorData> dataArr) {
        // Polyline 좌표 지정.
        mapView.removeAllPolylines();
        for(int i=0; i<dataArr.size(); i++) { // 매 CoorData마다 폴리라인 객체 생성
            MapPolyline polyline = new MapPolyline();
            polyline.setLineColor(Color.argb(128, i*10, 51, 0));
            polyline.setTag(1000);
            for(int x=0; x<dataArr.get(i).Coords.length/2; x++) {
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(dataArr.get(i).Coords[x*2+1]), Double.parseDouble(dataArr.get(i).Coords[x*2])));
            }
            mapView.addPolyline(polyline); // 폴리라인 객체 지도에 올리기
        }

        // 지도뷰의 중심좌표와 줌레벨을 Polyline이 모두 나오도록 조정.
        mapView.fitMapViewAreaToShowAllPolylines();
    }
}