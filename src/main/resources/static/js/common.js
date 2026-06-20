﻿﻿﻿﻿/**
 * common.js — 全局公共脚本
 *
 * 职责：
 * 1. CSRF 保护：所有 POST/PUT/DELETE AJAX 请求自动注入 X-XSRF-TOKEN 请求头
 * 2. 深色模式初始化（主题切换由 layout.html 中的脚本处理，此处仅做防闪烁初始化）
 */

// ============================================================
// CSRF Token 工具
// ============================================================

/**
 * 从 Cookie 中读取指定名称的值
 * @param {string} name Cookie 名称
 * @returns {string|null}
 */
function getCookieValue(name) {
    const cookies = document.cookie.split(';');
    for (let i = 0; i < cookies.length; i++) {
        const cookie = cookies[i].trim();
        if (cookie.startsWith(name + '=')) {
            return decodeURIComponent(cookie.substring(name.length + 1));
        }
    }
    return null;
}

/**
 * 获取 CSRF Token（Spring Security 通过 XSRF-TOKEN Cookie 下发）
 * @returns {string|null}
 */
function getCsrfToken() {
    return getCookieValue('XSRF-TOKEN');
}

// ============================================================
// jQuery 全局 AJAX 配置：自动注入 CSRF Token
// ============================================================

$(document).ready(function () {
    // 对所有修改类 AJAX 请求（非 GET/HEAD/OPTIONS）注入 X-XSRF-TOKEN 头
    $.ajaxSetup({
        beforeSend: function (xhr, settings) {
            const method = (settings.type || 'GET').toUpperCase();
            if (!/^(GET|HEAD|OPTIONS|TRACE)$/.test(method)) {
                const token = getCsrfToken();
                if (token) {
                    xhr.setRequestHeader('X-XSRF-TOKEN', token);
                }
            }
        }
    });
});



// ============================================================
// SystemAlert: 统一系统弹窗/通知交互组件 (基于 SweetAlert2)
// ============================================================
window.SystemAlert = {
    success: function(msg, callback) {
        Swal.fire({
            icon: 'success',
            title: msg,
            timer: 1500,
            showConfirmButton: false
        }).then(function() {
            if (callback) callback();
        });
    },
    error: function(msg) {
        Swal.fire({
            icon: 'error',
            title: msg,
            confirmButtonColor: '#6366f1'
        });
    },
    warning: function(msg) {
        Swal.fire({
            icon: 'warning',
            title: msg,
            confirmButtonColor: '#6366f1'
        });
    },
    confirm: function(msg, confirmCallback) {
        Swal.fire({
            title: '确定执行此操作吗？',
            text: msg,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc2626',
            cancelButtonColor: '#94a3b8',
            confirmButtonText: '确定',
            cancelButtonText: '取消'
        }).then(function(result) {
            if (result.isConfirmed && confirmCallback) {
                confirmCallback();
            }
        });
    }
};


    SystemAlert.info = function(msg) {
        Swal.fire({
            icon: 'info',
            title: msg,
            timer: 2000,
            showConfirmButton: false
        });
    };

