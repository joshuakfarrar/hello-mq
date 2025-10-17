# hello mq

a simple app to learn about MQ

### Configure PowerShell Profile

Add the following to your PowerShell profile (run `notepad $PROFILE`):

```ps
[Console]::OutputEncoding = [Text.Encoding]::UTF8
```

### 2. Configure MQ

This demo uses IBM MQ 9.3

```shell
PS C:\Users\josh\g\hello-mq> crtmqm QM1
PS C:\Users\josh\g\hello-mq> strmqm QM1
PS C:\Users\josh\g\hello-mq> runmqsc QM1
```

Then enter these commands:
```mq
DEFINE QLOCAL(DEV.QUEUE.1)
DEFINE CHANNEL(DEV.APP.SVRCONN) CHLTYPE(SVRCONN)
ALTER QMGR CCSID(1208)
SET CHLAUTH(DEV.APP.SVRCONN) TYPE(BLOCKUSER) USERLIST('nobody')
REFRESH SECURITY TYPE(CONNAUTH)
end
```

### 3. Run the app!

You should see...

```shell
PS C:\Users\josh\g\hello-mq> .\mill.bat --no-server app.run
[65/72] app.compile
[65] [info] compiling 1 Scala source to C:\Users\josh\g\hello-mq\out\app\compile.dest\classes ...
[65] [info] done compiling
[72/72] app.run
[72] 🚀 Starting MQ demo...
[72] 📤 Sending: 'Hello MQ!'
[72] ✅ Message sent!
[72] 📥 Receiving from DEV.QUEUE.1...
[72] ✅ Received: 'Hello MQ!'
[72] 🎉 Demo complete!
[72/72] ============================== app.run ============================== 12s
```