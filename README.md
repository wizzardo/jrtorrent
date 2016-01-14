JRtorrent
=========

this project is a web-interface for rtorrent

![alt text][mobile-screenshot] 

#### How to run
```bash
git clone https://github.com/wizzardo/jrtorrent.git
cd jrtorrent
./gradlew run
```

#### Run with screen
```bash
cd $HOME/ && screen -dmS -X rtorrent
cd $HOME/jrtorrent && screen -dmS -X ./gradlew run
```

#### rtorrent Installation
```bash
sudo apt-get apt-get install -y rtorrent 
mkdir $HOME/downloads && cd $HOME
echo "scgi_port = localhost:5000" >> .rtorrent.rc 
echo "directory = $HOME/downloads" >> .rtorrent.rc
echo "session = $HOME/.rt-session/" >> .rtorrent.rc
mkdir $HOME/.rt-session/
```

#### src/main/resources/Config.groovy
```groovy
server {
    host = '0.0.0.0'
    port = 8080
    ioWorkersCount = 1
    workersCount = 1
    ttl = 5 * 60 * 1000
    context = 'jrt'
}

jrt {
    rtorrent {
        host = 'localhost'
        port = 5000
    }
}
```


[mobile-screenshot]: https://wizzardo.github.io/jrtorrent/img/mobile.png "mobile-screenshot"
[desktop-screenshot]: https://wizzardo.github.io/jrtorrent/img/desktop.png "desktop-screenshot"