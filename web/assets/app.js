(function () {
  'use strict';

  var token = localStorage.getItem('wo_token');
  var user = null;
  try {
    user = JSON.parse(localStorage.getItem('wo_user') || 'null');
  } catch (e) {
    user = null;
  }
  if (!token || !user) {
    location.replace('/index.html');
    return;
  }

  var ROLE_NAMES = { EMPLOYEE: '员工', MANAGER: '部门主管', ADMIN: '管理员' };
  var STATUS = {
    1: { name: '待审批', cls: 'warning' },
    2: { name: '审批中', cls: 'primary' },
    3: { name: '已通过', cls: 'success' },
    4: { name: '已驳回', cls: 'danger' },
    5: { name: '已撤销', cls: 'info' }
  };
  var TYPES = { 1: '请假', 2: '报销', 3: '采购', 4: '用印', 5: '其他' };
  var PRIORITY = { 1: '低', 2: '中', 3: '高' };
  var ACTION = {
    1: { name: '通过', cls: 'success' },
    2: { name: '驳回', cls: 'danger' },
    3: { name: '转交', cls: 'info' },
    4: { name: '加签', cls: 'warning' }
  };
  var TASK_STATUS = {
    0: { name: '待处理', cls: 'warning' },
    1: { name: '已通过', cls: 'success' },
    2: { name: '已驳回', cls: 'danger' },
    3: { name: '已转交', cls: 'info' },
    4: { name: '已失效', cls: 'info' }
  };
  var APPROVER_TYPES = {
    DEPT_MANAGER: '部门主管',
    DEPT_ROLE: '部门内角色',
    ROLE: '全局角色',
    USER: '指定用户',
    USER_LIST: '指定多人'
  };
  var ROLES = { EMPLOYEE: '员工', MANAGER: '部门主管', ADMIN: '管理员' };

  var state = {
    view: 'dashboard',
    orders: { page: 1, size: 10, total: 0, status: '', type: '', keyword: '' },
    todo: { page: 1, size: 10, total: 0 },
    logs: { page: 1, size: 10, total: 0, keyword: '' },
    flows: { keyword: '' }
  };

  var allUsers = [];
  var allDepts = [];
  var flowCache = {};
  var flowEditingId = null;
  var metaPromise = null;

  /* ==================== 基础工具 ==================== */

  function esc(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function truncate(text, max) {
    var str = String(text == null ? '' : text);
    return str.length > max ? str.slice(0, max) + '…' : str;
  }

  var toastTimer = null;
  function toast(message, type) {
    var el = document.getElementById('toast');
    el.textContent = message;
    el.className = 'toast show ' + (type || '');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () {
      el.className = 'toast';
    }, 2400);
  }

  function doLogout() {
    localStorage.removeItem('wo_token');
    localStorage.removeItem('wo_user');
    location.replace('/index.html');
  }

  async function api(path, options) {
    options = options || {};
    var headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
    headers.token = localStorage.getItem('wo_token') || '';

    var response;
    try {
      response = await fetch('/api' + path, {
        method: options.method || 'GET',
        headers: headers,
        body: options.body ? JSON.stringify(options.body) : undefined
      });
    } catch (e) {
      throw new Error('网络异常，请确认后端服务已启动');
    }

    if (response.status === 401) {
      doLogout();
      throw new Error('登录已失效，请重新登录');
    }

    var data = null;
    try {
      data = await response.json();
    } catch (e) {
      data = null;
    }
    if (!data) {
      throw new Error('服务响应异常，请稍后重试');
    }
    if (data.code !== 1) {
      throw new Error(data.msg || '请求失败');
    }
    return data.data;
  }

  async function loadMeta() {
    try {
      allUsers = (await api('/users')) || [];
    } catch (e) {
      allUsers = [];
    }
    if (user.role === 'ADMIN') {
      try {
        allDepts = (await api('/depts')) || [];
      } catch (e) {
        allDepts = [];
      }
    }
  }

  function userOptionsHtml(selectedValue, withPlaceholder) {
    var selectedIds = String(selectedValue || '')
      .split(',')
      .map(function (item) { return item.trim(); })
      .filter(Boolean);
    var html = withPlaceholder ? '<option value="">请选择人员</option>' : '';
    allUsers.forEach(function (item) {
      html += '<option value="' + item.id + '"'
        + (selectedIds.indexOf(String(item.id)) >= 0 ? ' selected' : '') + '>'
        + esc(item.name) + '（' + esc(item.deptName || '') + '）</option>';
    });
    return html;
  }

  function roleOptionsHtml(selected) {
    return Object.keys(ROLES).map(function (key) {
      return '<option value="' + key + '"' + (key === selected ? ' selected' : '') + '>'
        + ROLES[key] + '</option>';
    }).join('');
  }

  /* ==================== 视图切换 ==================== */

  function loadView(view) {
    state.view = view;
    document.querySelectorAll('.view').forEach(function (el) {
      el.classList.add('hidden');
    });
    var target = document.getElementById('view-' + view);
    if (target) {
      target.classList.remove('hidden');
    }
    document.querySelectorAll('.nav-item').forEach(function (nav) {
      nav.classList.toggle('active', nav.dataset.view === view);
    });

    if (view === 'dashboard') {
      loadStats();
    } else if (view === 'orders') {
      loadOrders(1);
    } else if (view === 'todo') {
      loadTodo(1);
    } else if (view === 'logs') {
      loadLogs(1);
    } else if (view === 'flows') {
      loadFlows();
    } else if (view === 'create') {
      resetCreateForm();
    }
  }

  function refreshAfterAction() {
    loadView(state.view);
    refreshTodoBadge();
  }

  /* ==================== 工作台 ==================== */

  async function loadStats() {
    try {
      var stats = await api('/work-orders/stats');
      document.getElementById('statsGrid').innerHTML = [
        statCard('工单总数', stats.total, 'total'),
        statCard('待审批', stats.pendingCount, 'pending'),
        statCard('审批中', stats.processingCount, 'processing'),
        statCard('已通过', stats.approvedCount, 'approved'),
        statCard('已驳回', stats.rejectedCount, 'rejected')
      ].join('');
      renderBars('byType', stats.byType);
      renderBars('byDept', stats.byDept);
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  function statCard(label, value, cls) {
    return '<div class="stat-card stat-' + cls + '">'
      + '<div class="label">' + label + '</div>'
      + '<div class="num">' + (value || 0) + '</div>'
      + '</div>';
  }

  function renderBars(id, items) {
    var el = document.getElementById(id);
    if (!items || !items.length) {
      el.innerHTML = '<div class="empty" style="padding: 24px 0;">暂无数据</div>';
      return;
    }
    var max = 1;
    items.forEach(function (item) {
      max = Math.max(max, item.count);
    });
    el.innerHTML = items.map(function (item) {
      var width = Math.round((item.count / max) * 100);
      return '<div class="bar-row">'
        + '<span class="bar-label" title="' + esc(item.name) + '">' + esc(item.name) + '</span>'
        + '<div class="bar-track"><div class="bar-fill" style="width:' + width + '%"></div></div>'
        + '<span class="bar-value">' + item.count + '</span>'
        + '</div>';
    }).join('');
  }

  /* ==================== 工单列表 ==================== */

  async function loadOrders(page) {
    var q = state.orders;
    q.page = page || 1;
    var params = new URLSearchParams({ page: q.page, size: q.size });
    if (q.status) params.set('status', q.status);
    if (q.type) params.set('type', q.type);
    if (q.keyword) params.set('keyword', q.keyword);

    try {
      var data = await api('/work-orders?' + params.toString());
      q.total = data.total;
      document.getElementById('orderTable').innerHTML = orderTableHtml(data.rows);
      renderPager(document.getElementById('orderPager'), q.page, data.total, q.size, 'orderPage');
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  async function loadTodo(page) {
    var q = state.todo;
    q.page = page || 1;
    var params = new URLSearchParams({ page: q.page, size: q.size });

    try {
      var data = await api('/work-orders/todo?' + params.toString());
      q.total = data.total;
      document.getElementById('todoTable').innerHTML = orderTableHtml(data.rows, true);
      renderPager(document.getElementById('todoPager'), q.page, data.total, q.size, 'todoPage');
      setTodoBadge(data.total);
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  function orderTableHtml(rows, isTodo) {
    if (!rows || !rows.length) {
      return '<div class="empty">' + (isTodo ? '暂无待审批工单' : '暂无工单') + '</div>';
    }
    return '<div class="table-wrap"><table class="table">'
      + '<thead><tr>'
      + '<th>工单号</th><th>标题</th><th>类型</th><th>优先级</th>'
      + '<th>申请人</th><th>部门</th><th>当前节点</th><th>当前审批人</th><th>状态</th><th>提交时间</th><th>操作</th>'
      + '</tr></thead><tbody>'
      + rows.map(orderRowHtml).join('')
      + '</tbody></table></div>';
  }

  function orderRowHtml(order) {
    var status = STATUS[order.status] || { name: '未知', cls: 'info' };
    var priorityClass = order.priority === 3 ? 'priority-high' : (order.priority === 2 ? 'priority-mid' : '');
    return '<tr>'
      + '<td class="mono">' + esc(order.orderNo) + '</td>'
      + '<td class="title-cell" onclick="openDetail(' + order.id + ')">' + esc(order.title) + '</td>'
      + '<td>' + (TYPES[order.type] || '未知') + '</td>'
      + '<td class="' + priorityClass + '">' + (PRIORITY[order.priority] || '未知') + '</td>'
      + '<td>' + esc(order.applicantName) + '</td>'
      + '<td>' + esc(order.deptName) + '</td>'
      + '<td>' + esc(order.currentNodeName || '—') + '</td>'
      + '<td>' + esc(order.approverName) + '</td>'
      + '<td><span class="tag tag-' + status.cls + '">' + status.name + '</span></td>'
      + '<td class="muted">' + esc(order.createTime) + '</td>'
      + '<td><button class="btn btn-link" onclick="openDetail(' + order.id + ')">详情</button></td>'
      + '</tr>';
  }

  function renderPager(el, page, total, size, fnName) {
    var pages = Math.max(1, Math.ceil(total / size));
    var current = Math.min(page, pages);
    el.innerHTML = '<span>共 ' + total + ' 条 · 第 ' + current + ' / ' + pages + ' 页</span>'
      + '<button class="btn btn-small" ' + (current <= 1 ? 'disabled' : '')
      + ' onclick="' + fnName + '(' + (current - 1) + ')">上一页</button>'
      + '<button class="btn btn-small" ' + (current >= pages ? 'disabled' : '')
      + ' onclick="' + fnName + '(' + (current + 1) + ')">下一页</button>';
  }

  /* ==================== 工单详情与审批 ==================== */

  async function openDetail(id) {
    await metaPromise;
    try {
      var data = await api('/work-orders/' + id);
      renderDetail(data);
      document.getElementById('detailMask').classList.add('show');
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  function renderDetail(data) {
    var order = data.order;
    var status = STATUS[order.status] || { name: '未知', cls: 'info' };

    var timeline = (data.approvals || []).map(function (item) {
      var action = ACTION[item.action] || { name: '操作', cls: 'info' };
      return '<div class="timeline-item ' + (item.action === 2 ? 'timeline-reject' : '') + '">'
        + '<div class="timeline-head">'
        + '<strong>' + esc(item.approverName) + '</strong>'
        + '<span class="tag tag-' + action.cls + '">' + action.name + '</span>'
        + (item.round ? '<span class="muted">第 ' + item.round + ' 轮</span>' : '')
        + '<span class="muted">' + esc(item.createTime) + '</span>'
        + '</div>'
        + '<div class="timeline-comment">' + esc(item.comment || '—') + '</div>'
        + '</div>';
    }).join('');
    if (!timeline) {
      timeline = '<div class="muted">暂无审批记录</div>';
    }

    var currentTasks = (data.tasks || []).filter(function (task) {
      return task.round === order.round;
    });
    var taskHtml = currentTasks.length ? '<div class="task-list">'
      + currentTasks.map(function (task) {
        var taskState = TASK_STATUS[task.status] || { name: '未知', cls: 'info' };
        return '<div class="task-item">'
          + '<span class="tag tag-' + taskState.cls + '">' + taskState.name + '</span>'
          + '<span class="task-node">' + esc(task.nodeName || '') + '</span>'
          + '<span class="task-user">' + esc(task.approverName) + '</span>'
          + (task.addSign === 1 ? '<span class="tag tag-warning">加签</span>' : '')
          + '<span class="task-comment" title="' + esc(task.comment || '') + '">'
          + esc(truncate(task.comment || '', 30)) + '</span>'
          + '</div>';
      }).join('') + '</div>'
      : '<div class="muted">当前轮次暂无审批任务</div>';

    var approveBox = '';
    if (data.canApprove) {
      approveBox = '<div class="approve-box">'
        + '<h4>审批操作</h4>'
        + '<textarea id="approveComment" maxlength="200" placeholder="填写审批意见（驳回时必填）"></textarea>'
        + '<div class="form-actions">'
        + '<button class="btn btn-danger" onclick="doApprove(' + order.id + ', 2)">驳回</button>'
        + '<button class="btn btn-primary" onclick="doApprove(' + order.id + ', 1)">通过</button>'
        + '</div></div>';
    }

    var extraActions = '';
    if (data.canTransfer || data.canAddSign) {
      extraActions = '<div class="approve-box"><h4>协作操作</h4><div class="grid-2">';
      if (data.canTransfer) {
        extraActions += '<div class="form-row"><label>转交给</label>'
          + '<select class="select" id="transferUser">' + userOptionsHtml('', true) + '</select>'
          + '<button class="btn" style="margin-top: 8px;" onclick="doTransfer(' + order.id + ')">确认转交</button>'
          + '</div>';
      }
      if (data.canAddSign) {
        extraActions += '<div class="form-row"><label>加签人员（可多选）</label>'
          + '<select class="select multi-select" id="addSignUsers" multiple size="4">'
          + userOptionsHtml('', false) + '</select>'
          + '<button class="btn" style="margin-top: 8px;" onclick="doAddSign(' + order.id + ')">确认加签</button>'
          + '</div>';
      }
      extraActions += '</div></div>';
    }

    var withdrawBox = '';
    if (data.canWithdraw) {
      withdrawBox = '<div class="form-actions" style="margin-top: 14px;">'
        + '<button class="btn btn-danger" onclick="doWithdraw(' + order.id + ')">撤销工单</button>'
        + '</div>';
    }

    var resubmitBox = '';
    if (data.canResubmit) {
      resubmitBox = '<div class="form-actions" style="margin-top: 14px;">'
        + '<button class="btn btn-primary" onclick="doResubmit(' + order.id + ')">重新提交</button>'
        + '</div>';
    }

    document.getElementById('detailModal').innerHTML =
      '<div class="modal-header">'
      + '<h3>工单详情</h3>'
      + '<button class="modal-close" onclick="closeDetail()">&times;</button>'
      + '</div>'
      + '<div class="detail-grid">'
      + detailItem('工单号', order.orderNo)
      + '<div class="detail-item"><span class="detail-label">状态</span>'
      + '<span class="tag tag-' + status.cls + '">' + status.name + '</span></div>'
      + detailItem('工单类型', TYPES[order.type] || '未知')
      + detailItem('优先级', PRIORITY[order.priority] || '未知')
      + detailItem('申请人', order.applicantName)
      + detailItem('所属部门', order.deptName)
      + detailItem('审批流程', order.flowName || '—')
      + detailItem('当前节点', order.currentNodeName || '—')
      + detailItem('当前审批人', order.approverName)
      + detailItem('审批轮次', '第 ' + (order.round || 1) + ' 轮')
      + detailItem('提交时间', order.createTime)
      + detailItem('更新时间', order.updateTime)
      + '</div>'
      + '<h4>工单内容</h4>'
      + '<div class="detail-content">' + esc(order.content || '') + '</div>'
      + '<h4>当前轮次任务</h4>'
      + taskHtml
      + '<h4>审批记录</h4>'
      + '<div class="timeline">' + timeline + '</div>'
      + approveBox
      + extraActions
      + withdrawBox
      + resubmitBox;
  }

  function detailItem(label, value) {
    return '<div class="detail-item"><span class="detail-label">' + label + '</span>'
      + '<span>' + esc(value == null ? '—' : value) + '</span></div>';
  }

  function closeDetail() {
    document.getElementById('detailMask').classList.remove('show');
  }

  async function doApprove(id, action) {
    var commentEl = document.getElementById('approveComment');
    var comment = commentEl ? commentEl.value.trim() : '';

    if (action === 2 && !comment) {
      toast('驳回时必须填写审批意见', 'error');
      if (commentEl) {
        commentEl.focus();
      }
      return;
    }

    try {
      await api('/work-orders/' + id + '/approve', {
        method: 'POST',
        body: { action: action, comment: comment }
      });
      toast(action === 1 ? '审批通过' : '已驳回该工单', 'success');
      closeDetail();
      refreshAfterAction();
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  async function doWithdraw(id) {
    if (!window.confirm('确定撤销该工单吗？撤销后不可恢复。')) {
      return;
    }
    try {
      await api('/work-orders/' + id + '/withdraw', { method: 'PUT' });
      toast('工单已撤销', 'success');
      closeDetail();
      refreshAfterAction();
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  async function doTransfer(id) {
    var select = document.getElementById('transferUser');
    var toUserId = select ? Number(select.value) : 0;
    if (!toUserId) {
      toast('请选择转交人员', 'error');
      return;
    }
    try {
      await api('/work-orders/' + id + '/transfer', {
        method: 'POST',
        body: { toUserId: toUserId }
      });
      toast('已转交', 'success');
      closeDetail();
      refreshAfterAction();
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  async function doAddSign(id) {
    var select = document.getElementById('addSignUsers');
    var userIds = select
      ? Array.from(select.selectedOptions).map(function (option) { return Number(option.value); })
      : [];
    if (!userIds.length) {
      toast('请选择加签人员', 'error');
      return;
    }
    try {
      await api('/work-orders/' + id + '/add-sign', {
        method: 'POST',
        body: { userIds: userIds }
      });
      toast('加签成功', 'success');
      closeDetail();
      refreshAfterAction();
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  async function doResubmit(id) {
    if (!window.confirm('确定重新提交该工单吗？将从流程第一个节点重新审批。')) {
      return;
    }
    try {
      await api('/work-orders/' + id + '/resubmit', { method: 'POST' });
      toast('已重新提交', 'success');
      closeDetail();
      refreshAfterAction();
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  /* ==================== 新建工单 ==================== */

  function resetCreateForm() {
    var form = document.getElementById('createForm');
    if (!form) return;
    form.reset();
    document.getElementById('createType').value = '3';
    document.getElementById('createPriority').value = '2';
  }

  async function submitCreate(event) {
    event.preventDefault();
    var title = document.getElementById('createTitle').value.trim();
    var type = Number(document.getElementById('createType').value);
    var priority = Number(document.getElementById('createPriority').value);
    var content = document.getElementById('createContent').value.trim();

    if (title.length < 2 || title.length > 50) {
      toast('工单标题长度须为 2-50 个字符', 'error');
      return;
    }
    if (content.length < 5 || content.length > 1000) {
      toast('工单内容长度须为 5-1000 个字符', 'error');
      return;
    }

    var button = document.getElementById('createSubmitBtn');
    button.disabled = true;
    button.textContent = '提交中...';

    var requestId = (window.crypto && crypto.randomUUID)
      ? crypto.randomUUID()
      : 'req-' + Date.now() + '-' + Math.random().toString(16).slice(2);

    try {
      await api('/work-orders', {
        method: 'POST',
        headers: { 'X-Request-Id': requestId },
        body: { title: title, type: type, priority: priority, content: content }
      });
      toast('工单提交成功，已进入审批流程', 'success');
      resetCreateForm();
      refreshTodoBadge();
      loadView('orders');
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      button.disabled = false;
      button.textContent = '提交工单';
    }
  }

  /* ==================== 流程配置 ==================== */

  async function loadFlows() {
    try {
      var keyword = state.flows.keyword;
      var path = '/flows' + (keyword ? '?keyword=' + encodeURIComponent(keyword) : '');
      var flows = (await api(path)) || [];
      flowCache = {};
      flows.forEach(function (flow) {
        flowCache[flow.id] = flow;
      });
      document.getElementById('flowTable').innerHTML = flowTableHtml(flows);
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  function flowTableHtml(flows) {
    if (!flows || !flows.length) {
      return '<div class="empty">暂无流程配置</div>';
    }
    return '<div class="table-wrap"><table class="table">'
      + '<thead><tr>'
      + '<th>流程名称</th><th>编码</th><th>适用范围</th><th>节点</th><th>状态</th><th>操作</th>'
      + '</tr></thead><tbody>'
      + flows.map(function (flow) {
        var enabled = flow.status === 1;
        var nodes = (flow.nodes || []).map(function (node) { return node.nodeName; }).join(' → ');
        return '<tr>'
          + '<td>' + esc(flow.name) + '</td>'
          + '<td class="mono">' + esc(flow.code) + '</td>'
          + '<td>' + esc((flow.bizTypeName || '通用') + ' · ' + (flow.deptName || '不限部门')) + '</td>'
          + '<td class="flow-nodes-summary">' + esc(nodes || '—') + '</td>'
          + '<td><span class="tag tag-' + (enabled ? 'success' : 'info') + '">'
          + (enabled ? '启用' : '停用') + '</span></td>'
          + '<td>'
          + '<button class="btn btn-link" onclick="editFlow(' + flow.id + ')">编辑</button> '
          + '<button class="btn btn-link" onclick="toggleFlow(' + flow.id + ', ' + (enabled ? 0 : 1) + ')">'
          + (enabled ? '停用' : '启用') + '</button>'
          + '</td></tr>';
      }).join('') + '</tbody></table></div>';
  }

  async function openFlowModal(flowId) {
    await metaPromise;
    var flow = flowId ? flowCache[flowId] : null;
    flowEditingId = flow ? flow.id : null;

    var typeOptions = '<option value="">通用（兜底）</option>';
    Object.keys(TYPES).forEach(function (key) {
      typeOptions += '<option value="' + key + '">' + TYPES[key] + '</option>';
    });
    var deptOptions = '<option value="">不限部门</option>';
    allDepts.forEach(function (dept) {
      deptOptions += '<option value="' + dept.id + '">' + esc(dept.name) + '</option>';
    });

    document.getElementById('flowModal').innerHTML =
      '<div class="modal-header">'
      + '<h3>' + (flow ? '编辑流程' : '新建流程') + '</h3>'
      + '<button class="modal-close" onclick="closeFlowModal()">&times;</button>'
      + '</div>'
      + '<div class="form-grid">'
      + '<div class="form-row"><label>流程名称</label><input class="input" id="flowName" maxlength="30" placeholder="如：采购审批流程"></div>'
      + '<div class="form-row"><label>流程编码</label><input class="input" id="flowCode" maxlength="30" placeholder="如：PURCHASE"></div>'
      + '<div class="form-row"><label>状态</label><select class="select" id="flowStatus">'
      + '<option value="1">启用</option><option value="0">停用</option></select></div>'
      + '<div class="form-row"><label>适用工单类型</label><select class="select" id="flowBizType">' + typeOptions + '</select></div>'
      + '<div class="form-row"><label>适用部门</label><select class="select" id="flowDept">' + deptOptions + '</select></div>'
      + '<div class="form-row"><label>流程说明</label><input class="input" id="flowDesc" maxlength="100" placeholder="可选"></div>'
      + '</div>'
      + '<div class="flow-section-title"><span>审批节点（按顺序流转）</span>'
      + '<button class="btn btn-small" onclick="addFlowNode()">添加节点</button></div>'
      + '<div class="node-editor" id="flowNodeEditor"></div>'
      + '<div class="form-actions" style="margin-top: 16px;">'
      + '<button class="btn" onclick="closeFlowModal()">取消</button>'
      + '<button class="btn btn-primary" onclick="saveFlow()">保存流程</button>'
      + '</div>';

    document.getElementById('flowName').value = flow ? flow.name : '';
    document.getElementById('flowCode').value = flow ? flow.code : '';
    document.getElementById('flowStatus').value = flow ? String(flow.status) : '1';
    document.getElementById('flowBizType').value = flow && flow.bizType != null ? String(flow.bizType) : '';
    document.getElementById('flowDept').value = flow && flow.deptId != null ? String(flow.deptId) : '';
    document.getElementById('flowDesc').value = flow && flow.description ? flow.description : '';

    var editor = document.getElementById('flowNodeEditor');
    var nodes = flow && flow.nodes && flow.nodes.length
      ? flow.nodes
      : [{ nodeName: '', approverType: 'DEPT_MANAGER', multiMode: 'OR', allowSelf: 0, optionalNode: 0 }];
    editor.innerHTML = nodes.map(nodeRowHtml).join('');

    document.getElementById('flowMask').classList.add('show');
  }

  function nodeRowHtml(node, index) {
    var type = node.approverType || 'DEPT_MANAGER';
    return '<div class="node-row" data-node-id="' + (node.id || '') + '">'
      + '<div class="node-line">'
      + '<span class="node-index">' + (index + 1) + '</span>'
      + '<input class="input node-name" maxlength="30" placeholder="节点名称，如：部门主管审批" value="' + esc(node.nodeName || '') + '">'
      + '<select class="select node-type" onchange="changeNodeType(this)">'
      + Object.keys(APPROVER_TYPES).map(function (key) {
        return '<option value="' + key + '"' + (key === type ? ' selected' : '') + '>'
          + APPROVER_TYPES[key] + '</option>';
      }).join('')
      + '</select>'
      + '<span class="node-value">' + nodeValueHtml(node) + '</span>'
      + '<select class="select node-mode">'
      + '<option value="OR"' + (node.multiMode !== 'AND' ? ' selected' : '') + '>或签</option>'
      + '<option value="AND"' + (node.multiMode === 'AND' ? ' selected' : '') + '>会签</option>'
      + '</select>'
      + '<button class="btn btn-small btn-danger" onclick="removeFlowNode(this)">删除</button>'
      + '</div>'
      + '<div class="node-line sub">'
      + '<label class="checkbox-label"><input type="checkbox" class="node-allow-self"'
      + (node.allowSelf === 1 ? ' checked' : '') + '> 允许申请人自己审批</label>'
      + '<label class="checkbox-label"><input type="checkbox" class="node-optional"'
      + (node.optionalNode === 1 ? ' checked' : '') + '> 找不到审批人时自动跳过</label>'
      + '<span class="hint">部门主管=申请人所在部门主管；角色=全局指定角色；用户=指定人员</span>'
      + '</div></div>';
  }

  function nodeValueHtml(node) {
    var type = node.approverType || 'DEPT_MANAGER';
    var value = node.approverValue || '';
    if (type === 'DEPT_MANAGER') {
      return '<span class="hint">申请人所在部门主管</span>';
    }
    if (type === 'DEPT_ROLE' || type === 'ROLE') {
      return '<select class="select node-value-select">' + roleOptionsHtml(value) + '</select>';
    }
    if (type === 'USER') {
      return '<select class="select node-value-select">' + userOptionsHtml(value, true) + '</select>';
    }
    if (type === 'USER_LIST') {
      return '<select class="select node-value-select multi-select" multiple size="3">'
        + userOptionsHtml(value, false) + '</select>';
    }
    return '';
  }

  function changeNodeType(select) {
    var row = select.closest('.node-row');
    var valueCell = row.querySelector('.node-value');
    valueCell.innerHTML = nodeValueHtml({ approverType: select.value, approverValue: '' });
  }

  function addFlowNode() {
    var editor = document.getElementById('flowNodeEditor');
    editor.insertAdjacentHTML('beforeend', nodeRowHtml({
      nodeName: '',
      approverType: 'DEPT_MANAGER',
      multiMode: 'OR',
      allowSelf: 0,
      optionalNode: 0
    }, editor.children.length));
  }

  function removeFlowNode(button) {
    var row = button.closest('.node-row');
    if (!row) return;
    row.remove();
    document.querySelectorAll('#flowNodeEditor .node-index').forEach(function (el, index) {
      el.textContent = index + 1;
    });
  }

  function closeFlowModal() {
    document.getElementById('flowMask').classList.remove('show');
    flowEditingId = null;
  }

  async function saveFlow() {
    var name = document.getElementById('flowName').value.trim();
    var code = document.getElementById('flowCode').value.trim();
    if (!name) {
      toast('请输入流程名称', 'error');
      return;
    }
    if (!/^[A-Za-z0-9_]{2,30}$/.test(code)) {
      toast('流程编码须为 2-30 位字母、数字或下划线', 'error');
      return;
    }

    var rows = Array.from(document.querySelectorAll('#flowNodeEditor .node-row'));
    if (!rows.length) {
      toast('至少需要一个审批节点', 'error');
      return;
    }

    var nodes = [];
    for (var i = 0; i < rows.length; i++) {
      var row = rows[i];
      var type = row.querySelector('.node-type').value;
      var nodeName = row.querySelector('.node-name').value.trim();
      if (!nodeName) {
        toast('第 ' + (i + 1) + ' 个节点缺少名称', 'error');
        return;
      }
      var value = null;
      var valueSelect = row.querySelector('.node-value-select');
      if (type === 'DEPT_ROLE' || type === 'ROLE' || type === 'USER') {
        value = valueSelect ? valueSelect.value : '';
        if (!value) {
          toast('节点【' + nodeName + '】请选择审批人', 'error');
          return;
        }
      } else if (type === 'USER_LIST') {
        var selected = valueSelect
          ? Array.from(valueSelect.selectedOptions).map(function (option) { return option.value; })
          : [];
        if (!selected.length) {
          toast('节点【' + nodeName + '】请选择加签人员', 'error');
          return;
        }
        value = selected.join(',');
      }
      nodes.push({
        id: row.dataset.nodeId ? Number(row.dataset.nodeId) : null,
        nodeOrder: i + 1,
        nodeName: nodeName,
        approverType: type,
        approverValue: value,
        multiMode: row.querySelector('.node-mode').value,
        allowSelf: row.querySelector('.node-allow-self').checked ? 1 : 0,
        optionalNode: row.querySelector('.node-optional').checked ? 1 : 0
      });
    }

    var bizTypeValue = document.getElementById('flowBizType').value;
    var deptValue = document.getElementById('flowDept').value;
    var payload = {
      id: flowEditingId,
      name: name,
      code: code,
      bizType: bizTypeValue ? Number(bizTypeValue) : null,
      deptId: deptValue ? Number(deptValue) : null,
      status: Number(document.getElementById('flowStatus').value),
      description: document.getElementById('flowDesc').value.trim(),
      nodes: nodes
    };

    try {
      await api('/flows', { method: 'POST', body: payload });
      toast('流程保存成功', 'success');
      closeFlowModal();
      loadFlows();
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  async function toggleFlow(id, status) {
    if (status === 0 && !window.confirm('确定停用该流程吗？停用后新建工单将匹配其他流程。')) {
      return;
    }
    try {
      await api('/flows/' + id + '/status?status=' + status, { method: 'PUT' });
      toast(status === 1 ? '流程已启用' : '流程已停用', 'success');
      loadFlows();
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  /* ==================== 操作日志 ==================== */

  async function loadLogs(page) {
    var q = state.logs;
    q.page = page || 1;
    var params = new URLSearchParams({ page: q.page, size: q.size });
    if (q.keyword) params.set('keyword', q.keyword);

    try {
      var data = await api('/logs?' + params.toString());
      q.total = data.total;
      document.getElementById('logTable').innerHTML = logTableHtml(data.rows);
      renderPager(document.getElementById('logPager'), q.page, data.total, q.size, 'logPage');
    } catch (e) {
      toast(e.message, 'error');
    }
  }

  function logTableHtml(rows) {
    if (!rows || !rows.length) {
      return '<div class="empty">暂无操作日志</div>';
    }
    return '<div class="table-wrap"><table class="table">'
      + '<thead><tr><th>操作时间</th><th>操作人</th><th>操作</th><th>方法</th><th>耗时</th><th>结果</th></tr></thead>'
      + '<tbody>' + rows.map(function (log) {
        return '<tr>'
          + '<td class="muted">' + esc(log.operateTime) + '</td>'
          + '<td>' + esc(log.operateUserName) + '</td>'
          + '<td>' + esc(log.description) + '</td>'
          + '<td class="mono">' + esc(log.methodName) + '</td>'
          + '<td>' + (log.costTime == null ? '-' : log.costTime + ' ms') + '</td>'
          + '<td class="muted" title="' + esc(log.returnValue) + '">'
          + esc(truncate(log.returnValue, 36)) + '</td>'
          + '</tr>';
      }).join('') + '</tbody></table></div>';
  }

  /* ==================== 待办角标 ==================== */

  function setTodoBadge(count) {
    var badge = document.getElementById('todoBadge');
    badge.textContent = count > 0 ? count : '';
  }

  async function refreshTodoBadge() {
    try {
      var data = await api('/work-orders/todo?page=1&size=1');
      setTodoBadge(data.total);
    } catch (e) {
      /* 角标失败不打扰用户 */
    }
  }

  /* ==================== 事件绑定 ==================== */

  window.orderPage = function (page) { loadOrders(page); };
  window.todoPage = function (page) { loadTodo(page); };
  window.logPage = function (page) { loadLogs(page); };
  window.openDetail = openDetail;
  window.closeDetail = closeDetail;
  window.doApprove = doApprove;
  window.doWithdraw = doWithdraw;
  window.doTransfer = doTransfer;
  window.doAddSign = doAddSign;
  window.doResubmit = doResubmit;
  window.editFlow = openFlowModal;
  window.toggleFlow = toggleFlow;
  window.addFlowNode = addFlowNode;
  window.removeFlowNode = removeFlowNode;
  window.changeNodeType = changeNodeType;
  window.saveFlow = saveFlow;
  window.closeFlowModal = closeFlowModal;

  document.querySelectorAll('.nav-item').forEach(function (item) {
    item.addEventListener('click', function () {
      loadView(item.dataset.view);
    });
  });

  document.getElementById('logoutBtn').addEventListener('click', doLogout);

  document.getElementById('orderSearchBtn').addEventListener('click', function () {
    state.orders.status = document.getElementById('orderStatus').value;
    state.orders.type = document.getElementById('orderType').value;
    state.orders.keyword = document.getElementById('orderKeyword').value.trim();
    loadOrders(1);
  });

  document.getElementById('orderResetBtn').addEventListener('click', function () {
    document.getElementById('orderStatus').value = '';
    document.getElementById('orderType').value = '';
    document.getElementById('orderKeyword').value = '';
    state.orders.status = '';
    state.orders.type = '';
    state.orders.keyword = '';
    loadOrders(1);
  });

  document.getElementById('orderKeyword').addEventListener('keydown', function (event) {
    if (event.key === 'Enter') {
      document.getElementById('orderSearchBtn').click();
    }
  });

  document.getElementById('createForm').addEventListener('submit', submitCreate);
  document.getElementById('createResetBtn').addEventListener('click', resetCreateForm);

  document.getElementById('logSearchBtn').addEventListener('click', function () {
    state.logs.keyword = document.getElementById('logKeyword').value.trim();
    loadLogs(1);
  });

  document.getElementById('logKeyword').addEventListener('keydown', function (event) {
    if (event.key === 'Enter') {
      document.getElementById('logSearchBtn').click();
    }
  });

  document.getElementById('flowSearchBtn').addEventListener('click', function () {
    state.flows.keyword = document.getElementById('flowKeyword').value.trim();
    loadFlows();
  });

  document.getElementById('flowKeyword').addEventListener('keydown', function (event) {
    if (event.key === 'Enter') {
      document.getElementById('flowSearchBtn').click();
    }
  });

  document.getElementById('flowCreateBtn').addEventListener('click', function () {
    openFlowModal(null);
  });

  document.getElementById('detailMask').addEventListener('click', function (event) {
    if (event.target === this) {
      closeDetail();
    }
  });

  document.getElementById('flowMask').addEventListener('click', function (event) {
    if (event.target === this) {
      closeFlowModal();
    }
  });

  /* ==================== 初始化 ==================== */

  document.getElementById('userName').textContent = user.name || user.username;
  document.getElementById('userRole').textContent = ROLE_NAMES[user.role] || user.role;
  if (user.role !== 'ADMIN') {
    document.getElementById('navLogs').classList.add('hidden');
    document.getElementById('navFlows').classList.add('hidden');
  }

  metaPromise = loadMeta();
  loadView('dashboard');
  refreshTodoBadge();
})();
