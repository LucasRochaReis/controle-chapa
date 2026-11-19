/* =========================================================
 * REGRA DE PRIORIDADE DO SCROLL VERTICAL (roda do mouse)
 *
 * Intercepta APENAS quando o usuário GIRAR A RODA DO MOUSE
 * no sentido vertical (cima/baixo). Nesses casos:
 *   - Move SEMPRE a página para VERTICAL
 *   - Bloqueia a tabela / barra horizontal de "roubar" o foco
 *
 * NÃO interfere em:
 *   - Arrastar polegar da barra horizontal com o mouse (click+drag)
 *   - Shift + Roda (movimento horizontal explícito do usuário)
 *   - Arrastar dedo horizontal no touch / touchpad
 * ========================================================= */
(function priorityVerticalWheelOnly() {
  function getVerticalScroller() {
    var content = document.querySelector('.content');
    if (content && content.scrollHeight > content.clientHeight + 2) {
      return content;
    }
    return (document.scrollingElement || document.documentElement || window);
  }

  function onWheel(event) {
    if (!event) return;
    var dy = event.deltaY || 0;
    // Só intercepta se o movimento da roda é VERTICAL (usuário girou p/ cima ou p/ baixo)
    if (Math.abs(dy) < 1) {
      // Movimento puramente horizontal (Shift+wheel etc) = FLUXO NORMAL, não toca.
      return;
    }

    // ===== Regra efetiva: prioridade 100% vertical =====
    // Impede que qualquer outro listener (ex: barra horizontal, tabela) roube o evento
    event.stopImmediatePropagation();

    var mode = event.deltaMode || 0;
    var step;
    if (mode === 0) {
      step = dy; // pixels (Chrome/Firefox moderno)
    } else if (mode === 1) {
      step = dy * 24; // lines → ~24px cada
    } else {
      step = dy * Math.max(400, Math.round(window.innerHeight * 0.7)); // pages
    }

    var target = getVerticalScroller();
    if (target === window) {
      window.scrollBy(0, step);
    } else if (typeof target.scrollBy === 'function') {
      target.scrollBy(0, step);
    } else {
      target.scrollTop = (target.scrollTop || 0) + step;
    }

    // Cancela o comportamento padrão (que muitas vezes dispersava para o X)
    if (event.cancelable) event.preventDefault();
    return false;
  }

  // CAPTURE=true: pega o evento PRIMEIRO, antes de qualquer script de tabela/barra
  try {
    window.addEventListener('wheel', onWheel, { capture: true, passive: false });
    window.addEventListener('mousewheel', onWheel, { capture: true, passive: false });
  } catch (e) {
    window.attachEvent && window.attachEvent('onmousewheel', onWheel);
  }
})();

document.addEventListener('DOMContentLoaded', function () {
  var sidebarToggle = document.getElementById('sidebarToggle');
  var sidebarToggleIcon = document.getElementById('sidebarToggleIcon');
  var sidebarStorageKey = 'controle-papel-sidebar-collapsed';
  var body = document.body;

  function refreshSidebarToggleState() {
    var collapsed = body.classList.contains('sidebar-collapsed');
    if (sidebarToggle) {
      sidebarToggle.setAttribute('title', collapsed ? 'Abrir menu' : 'Recolher menu');
      sidebarToggle.setAttribute('aria-label', collapsed ? 'Abrir menu' : 'Recolher menu');
    }
    if (sidebarToggleIcon) {
      sidebarToggleIcon.textContent = collapsed ? '›' : '‹';
    }
  }

  if (localStorage.getItem(sidebarStorageKey) === 'true') {
    body.classList.add('sidebar-collapsed');
  }
  refreshSidebarToggleState();

  if (sidebarToggle) {
    sidebarToggle.addEventListener('click', function () {
      body.classList.toggle('sidebar-collapsed');
      localStorage.setItem(sidebarStorageKey, body.classList.contains('sidebar-collapsed'));
      refreshSidebarToggleState();
    });
  }

  var menuGroupToggles = document.querySelectorAll('[data-menu-group-toggle]');
  menuGroupToggles.forEach(function (btn) {
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      var group = btn.closest('.nav-group');
      if (!group) return;
      group.classList.toggle('open');
      var collapse = group.querySelector(':scope > .nav-group-collapse');
      if (collapse) collapse.classList.toggle('show', group.classList.contains('open'));
    });
  });

  var rows = document.querySelectorAll('.clickable-row[data-href]');
  rows.forEach(function (row) {
    row.addEventListener('click', function (event) {
      var target = event.target;
      while (target && target !== row) {
        var tag = target.tagName ? target.tagName.toLowerCase() : '';
        if (tag === 'a' || tag === 'button' || tag === 'input' || tag === 'select' || tag === 'textarea') {
          return;
        }
        target = target.parentElement;
      }
      var href = row.getAttribute('data-href');
      if (href) {
        window.location.href = href;
      }
    });
  });

  var scrollContainers = Array.prototype.slice.call(document.querySelectorAll('.table-responsive'));
  var viewportScrollbar = document.createElement('div');
  var viewportScrollbarInner = document.createElement('div');
  var activeScrollContainer = null;
  var syncingFromViewport = false;
  var syncingFromContainer = false;

  viewportScrollbar.className = 'viewport-x-scrollbar';
  viewportScrollbarInner.className = 'viewport-x-scrollbar-inner';
  viewportScrollbar.appendChild(viewportScrollbarInner);
  document.body.appendChild(viewportScrollbar);

  // (NÃO bloqueia mais pointerEvents — o usuário quer arrastar o polegar com o mouse!)
  // A roda do mouse é o único interceptado via listener global acima.

  function getVisibleWidth(rect) {
    var visibleLeft = Math.max(rect.left, 0);
    var visibleRight = Math.min(rect.right, window.innerWidth);
    return Math.max(0, visibleRight - visibleLeft);
  }

  function getVisibleHeight(rect) {
    var visibleTop = Math.max(rect.top, 0);
    var visibleBottom = Math.min(rect.bottom, window.innerHeight);
    return Math.max(0, visibleBottom - visibleTop);
  }

  function resolveActiveScrollContainer() {
    var bestContainer = null;
    var bestArea = 0;

    scrollContainers.forEach(function (container) {
      if (container.scrollWidth <= container.clientWidth + 1) {
        return;
      }

      var rect = container.getBoundingClientRect();
      if (rect.bottom <= 0 || rect.top >= window.innerHeight) {
        return;
      }

      var area = getVisibleWidth(rect) * getVisibleHeight(rect);
      if (area > bestArea) {
        bestArea = area;
        bestContainer = container;
      }
    });

    return bestContainer;
  }

  function syncViewportScrollbar() {
    activeScrollContainer = resolveActiveScrollContainer();

    if (!activeScrollContainer) {
      viewportScrollbar.classList.remove('is-visible');
      viewportScrollbar.scrollLeft = 0;
      return;
    }

    var containerRect = activeScrollContainer.getBoundingClientRect();
    var left = Math.max(0, Math.round(containerRect.left));
    var width = Math.max(0, activeScrollContainer.clientWidth);

    viewportScrollbar.style.left = left + 'px';
    viewportScrollbar.style.right = 'auto';
    viewportScrollbar.style.width = width + 'px';
    viewportScrollbarInner.style.width = activeScrollContainer.scrollWidth + 'px';
    viewportScrollbar.classList.add('is-visible');

    if (!syncingFromContainer) {
      syncingFromViewport = true;
      viewportScrollbar.scrollLeft = activeScrollContainer.scrollLeft;
      syncingFromViewport = false;
    }
  }

  viewportScrollbar.addEventListener('scroll', function () {
    if (!activeScrollContainer || syncingFromViewport) {
      return;
    }
    syncingFromContainer = true;
    activeScrollContainer.scrollLeft = viewportScrollbar.scrollLeft;
    syncingFromContainer = false;
  });

  scrollContainers.forEach(function (container) {
    container.addEventListener('scroll', function () {
      if (container !== activeScrollContainer || syncingFromContainer) {
        return;
      }
      syncingFromViewport = true;
      viewportScrollbar.scrollLeft = container.scrollLeft;
      syncingFromViewport = false;
    });
  });

  window.addEventListener('scroll', syncViewportScrollbar, { passive: true });
  window.addEventListener('resize', syncViewportScrollbar);
  syncViewportScrollbar();
});

/* =========================================================
 * CAMPO CLIENTE: AUTOCOMPLETE + NORMALIZACAO (MAIUSCULAS / SEM ACENTOS)
 *
 * Para inputs com atributo [data-cliente-input="true"]:
 *   - Em tempo real, converte para MAIUSCULAS e remove acentos
 *   - Faz busca por prefixo no endpoint /api/clientes?term=...
 *   - Exibe uma lista suspensa com cada cliente existente e um "x"
 *     para excluir os que estiverem errados
 * ========================================================= */
(function clienteAutocomplete() {
  var DEBOUNCE_MS = 220;
  var ACENTOS = /[\u0300-\u036f]/g;

  function normalizarNome(valor) {
    if (!valor) return '';
    var semEspacos = String(valor).trim();
    try {
      semEspacos = semEspacos.normalize('NFD').replace(ACENTOS, '');
    } catch (e) {
      semEspacos = semEspacos
        .replace(/[áàâãäå]/gi, 'a').replace(/[éèêë]/gi, 'e')
        .replace(/[íìîï]/gi, 'i').replace(/[óòôõö]/gi, 'o')
        .replace(/[úùûü]/gi, 'u').replace(/[ç]/gi, 'c')
        .replace(/[ñ]/gi, 'n');
    }
    return semEspacos.toUpperCase();
  }

  function baseCtx() {
    var ctxMeta = document.querySelector('meta[name="ctx"]');
    if (ctxMeta && ctxMeta.getAttribute('content')) {
      return ctxMeta.getAttribute('content').replace(/\/+$/, '') + '/';
    }
    var match = (window.location.pathname || '').match(/^\/[^\/]+/);
    return match ? match[0].replace(/\/+$/, '') + '/' : '/';
  }

  function csrfToken() {
    var meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.getAttribute('content') : '';
  }

  function csrfHeader() {
    var meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
  }

  function requisitar(metodo, url, cb) {
    var xhr = new XMLHttpRequest();
    xhr.open(metodo, url, true);
    xhr.setRequestHeader('Accept', 'application/json');
    if (metodo !== 'GET') {
      var token = csrfToken();
      if (token) xhr.setRequestHeader(csrfHeader(), token);
    }
    xhr.onload = function () {
      var data = null;
      try { data = JSON.parse(xhr.responseText || 'null'); } catch (e) { data = null; }
      cb(xhr.status, data);
    };
    xhr.onerror = function () { cb(0, null); };
    xhr.send();
  }

  function buscarClientes(term, cb) {
    var url = baseCtx() + 'api/clientes' + (term ? '?term=' + encodeURIComponent(term) : '');
    requisitar('GET', url, function (status, data) {
      cb(status >= 200 && status < 300 && Array.isArray(data) ? data : []);
    });
  }

  function criarCaixa(input) {
    var container = input.parentNode;
    if (container && window.getComputedStyle(container).position === 'static') {
      container.style.position = 'relative';
    }
    var box = document.createElement('div');
    box.className = 'cliente-autocomplete';
    box.style.display = 'none';
    container.appendChild(box);
    return box;
  }

  function limparCaixa(box) {
    box.style.display = 'none';
    while (box.firstChild) box.removeChild(box.firstChild);
  }

  function renderizar(box, itens, input) {
    while (box.firstChild) box.removeChild(box.firstChild);
    if (!itens || !itens.length) {
      box.style.display = 'none';
      return;
    }

    itens.forEach(function (item) {
      var linha = document.createElement('div');
      linha.className = 'cliente-autocomplete-item';
      // Evita que o clique na lista tire o foco do input antes do handler de click.
      linha.addEventListener('mousedown', function (ev) { ev.preventDefault(); });

      var nome = document.createElement('button');
      nome.type = 'button';
      nome.className = 'cliente-autocomplete-nome';
      nome.textContent = item.nome;
      nome.addEventListener('click', function () {
        input.value = item.nome;
        limparCaixa(box);
        input.focus();
      });

      var remover = document.createElement('button');
      remover.type = 'button';
      remover.className = 'cliente-autocomplete-remover';
      remover.title = 'Excluir cliente';
      remover.setAttribute('aria-label', 'Excluir cliente');
      remover.textContent = '\u00d7';
      remover.addEventListener('click', function () {
        excluirCliente(item, box, input);
      });

      linha.appendChild(nome);
      linha.appendChild(remover);
      box.appendChild(linha);
    });

    box.style.display = 'block';
  }

  function excluirCliente(item, box, input) {
    if (!window.confirm('Excluir o cliente "' + item.nome + '"?')) return;
    var url = baseCtx() + 'api/clientes/' + encodeURIComponent(item.id);
    requisitar('DELETE', url, function (status, data) {
      if (status >= 200 && status < 300 && data && data.sucesso) {
        var linhas = box.querySelectorAll('.cliente-autocomplete-item');
        for (var i = 0; i < linhas.length; i++) {
          var botao = linhas[i].querySelector('.cliente-autocomplete-nome');
          if (botao && botao.textContent === item.nome) {
            box.removeChild(linhas[i]);
            break;
          }
        }
        if (input.value.trim() === item.nome) input.value = '';
        if (!box.children.length) box.style.display = 'none';
      } else {
        window.alert((data && data.mensagem) ? data.mensagem : 'Nao foi possivel excluir este cliente.');
      }
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    var inputs = document.querySelectorAll('input[data-cliente-input="true"]');

    if (!inputs.length) return;

    inputs.forEach(function (input) {
      input.removeAttribute('list');
      input.setAttribute('autocomplete', 'off');
      input.spellcheck = false;

      var box = criarCaixa(input);

      var cursorNaPosicaoCorreta = function () {
        var len = input.value.length;
        try {
          if (input.setSelectionRange) input.setSelectionRange(len, len);
        } catch (e) {}
      };

      var ultimaChave = 0;
      var timer = null;

      function buscar(termo) {
        ultimaChave++;
        var minhaChave = ultimaChave;
        buscarClientes(termo, function (itens) {
          if (minhaChave !== ultimaChave) return;
          renderizar(box, itens, input);
        });
      }

      input.addEventListener('input', function () {
        var original = input.value;
        var normalizado = normalizarNome(original);
        if (normalizado !== original) {
          input.value = normalizado;
          cursorNaPosicaoCorreta();
        }
        if (timer) clearTimeout(timer);
        timer = setTimeout(function () { buscar(normalizado); }, DEBOUNCE_MS);
      });

      input.addEventListener('focus', function () {
        buscar(normalizarNome(input.value));
      });

      input.addEventListener('blur', function () {
        input.value = normalizarNome(input.value);
      });

      input.addEventListener('keydown', function (ev) {
        if (ev.key === 'Escape') limparCaixa(box);
      });

      // Fecha a lista ao clicar fora (mantendo-a aberta ao usar a barra de rolagem ou as opcoes).
      document.addEventListener('mousedown', function (ev) {
        if (box.style.display === 'none') return;
        if (box === ev.target || box.contains(ev.target)) return;
        if (input === ev.target) return;
        limparCaixa(box);
      });
    });
  });
})();

/* =========================================================
 * CAMPO OBSERVACAO: SUGESTOES (APENAS SUGESTAO, NAO FIXO)
 *
 * Para textareas com atributo [data-observacao-input="true"]:
 *   - Sugere textos de observacao ja utilizados em lancamentos
 *   - O campo continua livre: qualquer texto pode ser digitado
 *   - Ao clicar em uma sugestao, ela e preenchida no campo
 * ========================================================= */
(function observacaoAutocomplete() {
  var DEBOUNCE_MS = 220;

  function baseCtx() {
    var ctxMeta = document.querySelector('meta[name="ctx"]');
    if (ctxMeta && ctxMeta.getAttribute('content')) {
      return ctxMeta.getAttribute('content').replace(/\/+$/, '') + '/';
    }
    var match = (window.location.pathname || '').match(/^\/[^\/]+/);
    return match ? match[0].replace(/\/+$/, '') + '/' : '/';
  }

  function requisitar(url, cb) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.onload = function () {
      var data = null;
      try { data = JSON.parse(xhr.responseText || 'null'); } catch (e) { data = null; }
      cb(xhr.status, data);
    };
    xhr.onerror = function () { cb(0, null); };
    xhr.send();
  }

  function buscarObservacoes(term, cb) {
    var url = baseCtx() + 'api/observacoes' + (term ? '?term=' + encodeURIComponent(term) : '');
    requisitar(url, function (status, data) {
      cb(status >= 200 && status < 300 && Array.isArray(data) ? data : []);
    });
  }

  function criarCaixa(campo) {
    var container = campo.parentNode;
    if (container && window.getComputedStyle(container).position === 'static') {
      container.style.position = 'relative';
    }
    var box = document.createElement('div');
    box.className = 'observacao-autocomplete';
    box.style.display = 'none';
    container.appendChild(box);
    return box;
  }

  function limparCaixa(box) {
    box.style.display = 'none';
    while (box.firstChild) box.removeChild(box.firstChild);
  }

  function renderizar(box, itens, campo) {
    while (box.firstChild) box.removeChild(box.firstChild);
    if (!itens || !itens.length) {
      box.style.display = 'none';
      return;
    }

    itens.forEach(function (texto) {
      var linha = document.createElement('div');
      linha.className = 'observacao-autocomplete-item';
      linha.addEventListener('mousedown', function (ev) { ev.preventDefault(); });

      var botao = document.createElement('button');
      botao.type = 'button';
      botao.className = 'observacao-autocomplete-nome';
      botao.title = texto;
      botao.textContent = texto;
      botao.addEventListener('click', function () {
        campo.value = texto;
        limparCaixa(box);
        campo.focus();
      });

      linha.appendChild(botao);
      box.appendChild(linha);
    });

    box.style.display = 'block';
  }

  document.addEventListener('DOMContentLoaded', function () {
    var campos = document.querySelectorAll('textarea[data-observacao-input="true"]');

    if (!campos.length) return;

    campos.forEach(function (campo) {
      campo.setAttribute('autocomplete', 'off');
      campo.spellcheck = false;

      var box = criarCaixa(campo);
      var ultimaChave = 0;
      var timer = null;

      function buscar(termo) {
        ultimaChave++;
        var minhaChave = ultimaChave;
        buscarObservacoes(termo, function (itens) {
          if (minhaChave !== ultimaChave) return;
          renderizar(box, itens, campo);
        });
      }

      campo.addEventListener('input', function () {
        if (timer) clearTimeout(timer);
        timer = setTimeout(function () { buscar(campo.value.trim()); }, DEBOUNCE_MS);
      });

      campo.addEventListener('focus', function () {
        buscar(campo.value.trim());
      });

      campo.addEventListener('keydown', function (ev) {
        if (ev.key === 'Escape') limparCaixa(box);
      });

      document.addEventListener('mousedown', function (ev) {
        if (box.style.display === 'none') return;
        if (box === ev.target || box.contains(ev.target)) return;
        if (campo === ev.target) return;
        limparCaixa(box);
      });
    });
  });
})();
