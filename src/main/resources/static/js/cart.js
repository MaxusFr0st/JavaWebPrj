/**
 * Live cart badge via Server-Sent Events (async publish from server).
 */
(function () {
    const badge = document.getElementById('cart-badge');
    if (!badge) return;

    function setCount(value) {
        const n = parseInt(value, 10);
        if (!Number.isNaN(n)) {
            badge.textContent = n;
        }
    }

    if (typeof EventSource !== 'undefined') {
        const source = new EventSource('/cart/sse');
        source.addEventListener('cart-count', function (event) {
            setCount(event.data);
        });
        source.onerror = function () {
            source.close();
            fetch('/cart/count')
                .then(function (r) { return r.json(); })
                .then(function (data) { setCount(data.count); })
                .catch(function () {});
        };
    } else {
        fetch('/cart/count')
            .then(function (r) { return r.json(); })
            .then(function (data) { setCount(data.count); })
            .catch(function () {});
    }
})();
