# Notes on sysadmin stuff on the Linux server

Connect to server:
```shell
ssh root@s2595.rootserver.io

# once the user `aitutor` is setup:
ssh aitutor@s2595.rootserver.io
```

Initial setup:
```shell
apt update && apt upgrade -y

useradd -m -d /opt/aitutor -s /bin/bash aitutor

# 0) (Optional) Remove conflicting old packages
apt-get remove -y docker docker.io docker-doc docker-compose podman-docker containerd runc || true

# 1) Prereqs
apt-get update
apt-get install -y ca-certificates curl gnupg

# 2) Add Docker’s GPG key & repo for Debian 12 (bookworm)
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/debian bookworm stable" \
  > /etc/apt/sources.list.d/docker.list

# 3) Install Docker Engine + Buildx + Compose plugin
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 4) Enable and start
systemctl enable --now docker

# 5) Ensure docker group exists, add your user (replace if needed)
groupadd docker || true
usermod -aG docker aitutor

# 6) New shell for group to apply, quick test
su - aitutor -c 'docker run --rm hello-world || true'
```

Finish setup user `itutor`

```shell
passwd aitutor
# enter password twice

su - aitutor

usermod -aG sudo aitutor
```

Install more stuff (as aitutor using sudo)
```shell
sudo apt install nginx certbot python3-certbot-nginx ufw git -y
```

```shell
sudo systemctl stop nginx
```

```shell
sudo certbot certonly --standalone -d ai-tutor.obermuhlner.ch
```

Copy the nginx config from 
```shell
sudo cp nginx.conf /etc/nginx/nginx.conf
```

```shell
sudo systemctl start nginx
```


Set GitHub PAT created at https://github.com/settings/tokens
```shell
echo '<YOUR_PAT>' | docker login ghcr.io -u eobermuhlner --password-stdin
```

