# 프로젝트 폴더 구조
- `/app/src/main/java/com/example/project` - 자바 소스 코드의 루트 디렉토리
  - `/helper`
    - `HttpHelper` - 싱글톤 패턴을 사용하여 지정된 기본 URL에 대해 초기화된 Retrofit 클라이언트 인스턴스를 생성하고 관리
  - `/manager` - 데이터 관리를 위한 클래스(재사용 가능한 함수들을 모아 놓은 클래스)를 포함하는 폴더
    - `GalleryImageManager` - 갤러리에서 이미지 데이터(Uri, 사진 이름 등) 추출 및 관리하는 함수 제공
    - `Neo4jDatabaseManager` - 사이퍼쿼리 생성 함수 제공
    - `Neo4jDriverManager` - 드라이버 초기화 및 싱글톤 패턴을 사용하여 앱 전체에서 하나의 드라이버 인스턴스만 사용하도록 관리
    - `UriToFileConverter` - Uri 객체를 받아서 해당 URI의 콘텐츠를 읽어 임시 파일로 저장하는 기능 제공
  - `/model` - 데이터 모델 클래스(앱의 데이터 구조를 정의하는 클래스)를 포함하는 폴더
    - `Circle` - 사용자가 그린 원의 정보를 저장하기 위한 데이터 모델(원의 중심 좌표, 반지름)
    - `CircleDetectionResponse` - 서버에서 받은 응답을 위한 데이터 모델(응답 성공 여부 메시지, 원에서 분석된 객체 이름 리스트)
    - `Person` - 인물 얼굴 출력을 위한 데이터 모델(이름, 이미지)
    - `PhotoResponse` - 서버로부터 받은 사진 이름 데이터 모델()
  - `/service` - 네트워크 통신을 위한 클래스를 포함하는 폴더
    - `ApiService` - 이미지와 원 데이터를 서버에 업로드하고, 감지된 객체를 다른 서버로 전송하는 HTTP 요청을 처리하기 위한 Retrofit HTTP 메소드 선언을 포함
  - `/ui` - 액티비티와 프래그먼트 등 사용자 인터페이스를 위한 클래스를 포함하는 폴더
    - `/activity` - 앱의 액티비티를 포함하는 폴더
      - `CircleToSearchActivity` - circle to search 이미지 검색
      - `ImageDisplayActivity` - 이미지 줌 인, 아웃
      - `MainActivity` - 필요한 권한 요청
      - `PersonPhotosActivity` - 인물 얼굴 출력 
    - `/adapter` - 리사이클러뷰 어댑터 등 UI 컴포넌트를 위한 클래스를 포함하는 폴더
    - `/fragment` - 앱의 프래그먼트를 포함하는 폴더
      - `CompositeFragment` - 합성된 이미지 출력
      - `GraphFragment` - 지식 그래프 출력
      - `HomeFragment` - 갤러리의 모든 사진 출력(상단에 분석된 인물 얼굴 출력)
      - `SearchFragment` - 자연어로 이미지 검색
    - `/viewmodel` - 앱의 뷰모델을 포함하는 폴더
    - `CustomImageView` - 사진 위에 원을 그릴 수 있는 커스텀 뷰
