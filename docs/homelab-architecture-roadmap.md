## 기타 참조: 홈랩 아키텍처 구축 로드맵

### Synology NAS Homelab Architecture (without S3)

- `MinIO` (Docker on NAS)
    - S3 API와 100% 호환 가능한 오픈소스 Object Storage
    - S3를 대체하여 Terraform의 백엔드(tfstate) 저장소로 활용 가능
- `Gitea` (Docker on NAS)
    - 경량 Git 서버 (Github 대체재)
    - Gitea Actions(CI/CD), Container Registy(Docker Image Storage) 제공
- `k3s` or `minikube` (Docker/VM on NAS)
    - NAS 상에서 Kubernates 클러스터 실행

### 구축 의미

<aside>

💬 **“왜 AWS 프리티어 대신 굳이 NAS에 홈랩을 구축했는가”**

</aside>

- NAS 투자 비용은 이미 들어간 상황으로, AWS 프리티어는 중복 지출
- 당장 NAS 구매를 가정한다 하더라도 유지 비용면에서 장기적 이득으로 초기 투자 비용 유의미
- 향후 개인 프로젝트 R&D를 위한 Private Lab으로서의 가치 존재