# 프로젝트 초기 환경 셋업 가이드

## 프로젝트 디렉토리 구조 및 관리 전략

```text
grimault/
├── docs/                 # 노션 대신 작성할 작업 문서, DDL, 스키마 정의
├── data/                 # 네이버 가계부 원본 CSV/엑셀 (.gitignore 필수)
├── scripts/              # RAW 데이터 PostgreSQL 1차 적재/정제 스크립트
├── sql/                  # PostgreSQL RAW/Public Table DDLs 
├── backend/              # Kotlin / Spring Boot API 프로젝트
│   ├── src/
│   └── build.gradle.kts
├── frontend/             # (3주차에 추가될) 경량 Web UI
├── docker-compose.yml    # PostgreSQL 1기 + backend 구동 명세
└── README.md             # 프로젝트 소개 및 실행 가이드
```

- 신규 Gradle Project 생성하여 분리: 가계부 자체가 완결성 높은 단독 프로젝트 성격
- 배포 시 dockerfile 빌드 컨텍스트가 불필요한 의존성 충돌 없는 경량 관리면에서 유리

## 프로젝트 초기화

```bash
# 프로젝트 다운로드
curl "https://start.spring.io/starter.zip" \
  -d language=kotlin \
  -d type=gradle-project \
  -d javaVersion=17 \
  -d bootVersion=4.1.1 \
  -d dependencies=web,data-jpa,postgresql,validation \
  -d groupId=com.blustar \
  -d artifactId=grimault \
  -d name=grimault \
  -o grimault.zip
```

```bash
# 압축 해제
unzip grimault.zip -d ./grimault
rm grimault.zip
cd ./grimault
```

```bash
# 필수 작업 디렉토리 생성
mkdir data scripts docs sql
```

```bash
# 기본 Git 설정
git init
git branch -M main

# 개발환경 공통 gitignore 다운로드
curl -s "https://www.toptal.com/developers/gitignore/api/java,kotlin,gradle,macos,visualstudiocode" > .gitignore

# 금융 데이터 및 환경설정 파일 유출 방지
cat << 'EOF' >> .gitignore

### Local Data & Secrets ###
/data/
*.csv
*.tsv
*.xls
*.xlsx
.env
EOF
```

```bash
# 첫 커밋 및 원격 저장소 푸시
git add .
git commit -m "chore: initial commit for grimault project setup"

git remote add origin https://github.com/blue1769/grimault.git
git push -u origin main
```