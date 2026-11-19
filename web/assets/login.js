(function () {
  var form = document.getElementById('loginForm');
  var usernameInput = document.getElementById('username');
  var passwordInput = document.getElementById('password');
  var loginBtn = document.getElementById('loginBtn');

  // 已登录则直接进入系统
  if (localStorage.getItem('wo_token')) {
    location.replace('/home.html');
    return;
  }

  document.querySelectorAll('.demo-chip').forEach(function (chip) {
    chip.addEventListener('click', function () {
      usernameInput.value = chip.dataset.user;
      passwordInput.value = '123456';
      passwordInput.focus();
    });
  });

  form.addEventListener('submit', function (event) {
    event.preventDefault();
    var username = usernameInput.value.trim();
    var password = passwordInput.value;

    if (!username) {
      toast('请输入账号', 'error');
      usernameInput.focus();
      return;
    }
    if (!password) {
      toast('请输入密码', 'error');
      passwordInput.focus();
      return;
    }

    loginBtn.disabled = true;
    loginBtn.textContent = '登录中...';

    fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username, password: password })
    })
      .then(function (res) {
        return res.json().catch(function () {
          return { code: 500, msg: '服务异常，请稍后重试' };
        });
      })
      .then(function (data) {
        if (data.code !== 1) {
          toast(data.msg || '登录失败', 'error');
          return;
        }
        localStorage.setItem('wo_token', data.data.token);
        localStorage.setItem('wo_user', JSON.stringify(data.data.user));
        location.replace('/home.html');
      })
      .catch(function () {
        toast('网络异常，请确认后端服务已启动', 'error');
      })
      .finally(function () {
        loginBtn.disabled = false;
        loginBtn.textContent = '登 录';
      });
  });

  var toastTimer = null;
  function toast(message, type) {
    var el = document.getElementById('toast');
    if (!el) {
      el = document.createElement('div');
      el.id = 'toast';
      el.className = 'toast';
      document.body.appendChild(el);
    }
    el.textContent = message;
    el.className = 'toast show ' + (type || '');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      el.className = 'toast';
    }, 2200);
  }
})();
