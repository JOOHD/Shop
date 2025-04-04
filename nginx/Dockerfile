FROM nginx:latest

# 템플릿 파일 복사
COPY my-site.conf.template /etc/nginx/templates/my-site.conf.template

# envsubst 설치
RUN apt-get update && apt-get install -y gettext-base

# 환경 변수를 적용하여 설정 파일 생성 후 Nginx 시작
CMD ["/bin/bash", "-c", "envsubst '${NGINX_SERVER}' < /etc/nginx/templates/my-site.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]

