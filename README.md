# Hello MQ!

A simple app to learn about MQ

## Requirements

IBM MQ 9.3 (a trial version works just fine)

Check that MQ is installed locally with `dspmqver`:

```
PS C:\Users\josh\g\hello-mq> dspmqver
Name:        IBM MQ
Version:     9.3.0.0
Level:       p930-L220607.TRIAL
BuildType:   IKAP - (Production)
Platform:    IBM MQ for Windows (x64 platform)
Mode:        64-bit
O/S:         Windows 11 Unknown x64 Edition, Build 26100
InstName:    Installation1
InstDesc:
Primary:     Yes
InstPath:    C:\Program Files\IBM\MQ
DataPath:    C:\ProgramData\IBM\MQ
MaxCmdLevel: 930
LicenseType: Trial
```

## Getting Started

### 1. Configure your PowerShell Profile for UTF-8 Output

Add the following to your PowerShell profile (run `notepad $PROFILE`):

```ps
[Console]::OutputEncoding = [Text.Encoding]::UTF8
```

### 2. Create the MQ User

Because our example uses channel auth, we have to have a local user (or a GMSA in the real world) we can authorize to use the channel.

```
PS C:\Users\josh\g\hello-mq> $Password = ConvertTo-SecureString "password" -AsPlainText -Force
PS C:\Users\josh\g\hello-mq> New-LocalUser "app" -Password $Password -Description "MQ Application User" -PasswordNeverExpires
```

### 3. Configure MQ

First, let's create a new Queue Manager, `QM1`.

```
PS C:\Users\josh\g\hello-mq> crtmqm QM1
PS C:\Users\josh\g\hello-mq> strmqm QM1
PS C:\Users\josh\g\hello-mq> runmqsc QM1
```

Then enter these commands:
```mq
DEFINE QLOCAL(DEV.QUEUE.1)
DEFINE CHANNEL(DEV.APP.SVRCONN) CHLTYPE(SVRCONN)
ALTER QMGR CCSID(1208)

* Block the default 'nobody' user
SET CHLAUTH(DEV.APP.SVRCONN) TYPE(BLOCKUSER) USERLIST('nobody')

* Allow specific users (replace 'app' with your desired username)
SET CHLAUTH(DEV.APP.SVRCONN) TYPE(USERMAP) CLNTUSER('app') USERSRC(CHANNEL) CHCKCLNT(REQUIRED)

* Configure connection authentication
ALTER QMGR CONNAUTH('SYSTEM.DEFAULT.AUTHINFO.IDPWOS')
ALTER AUTHINFO('SYSTEM.DEFAULT.AUTHINFO.IDPWOS') AUTHTYPE(IDPWOS) CHCKCLNT(REQUIRED)

* Grant permissions to your user
SET AUTHREC PRINCIPAL('app') OBJTYPE(QMGR) AUTHADD(CONNECT,INQ)
SET AUTHREC PROFILE(DEV.QUEUE.1) OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(BROWSE,GET,INQ,PUT)

REFRESH SECURITY TYPE(CONNAUTH)
end
```

### 4. Run the app!

You should see...

```shell
PS C:\Users\josh\g\hello-mq> .\mill.bat app.run
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
