## 使用说明

1. 配置文件中填入正确的配置信息

    ```
    server.port=9000
    logging.file=./log.log
    level=info
    sync=true
    cron=0/10 59 7 * * ?

    date=2017-01-09
    train=G557,G401,G517,G403,G491,G69
    start=BJP
    end=ZDN
    seat=O
    name=**
    password=**
    ```

2. 启动Application.java
3. 登录 127.0.0.1:9000,输入乘车人信息
4. 点击登录，完成验证码输入，完成登录（登录状态为true）
5. 等待脚本自动执行（手动点击强制执行即可测试）
6. 进入购票验证码输入页面，等待刷出新验证么后输入...