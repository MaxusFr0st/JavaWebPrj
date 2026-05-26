/**
 * PayPal Standard Integration — waits for SDK, then renders buttons.
 */
window.initPayPalCheckout = function initPayPalCheckout() {
    const container = document.getElementById('paypal-button-container');
    if (!container) {
        return;
    }

    function showMessage(message, isError) {
        const el = document.getElementById('paypal-result-message');
        if (el) {
            el.textContent = message;
            el.className = isError ? 'text-danger small mt-2' : 'text-success small mt-2';
        }
    }

    if (typeof paypal === 'undefined') {
        showMessage('PayPal SDK failed to load. Check your client ID, internet connection, and browser console (F12).', true);
        return;
    }

    paypal.Buttons({
        style: {
            shape: 'rect',
            layout: 'vertical',
            color: 'gold',
            label: 'paypal'
        },

        async createOrder() {
            const response = await fetch('/checkout/paypal/orders', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin'
            });
            if (!response.ok) {
                const errBody = await response.json().catch(function () { return {}; });
                const msg = errBody.error || ('Server error ' + response.status);
                showMessage(msg, true);
                throw new Error(msg);
            }
            const orderData = await response.json();
            if (orderData.id) {
                return orderData.id;
            }
            const msg = orderData.error || JSON.stringify(orderData);
            showMessage('Could not start PayPal checkout: ' + msg, true);
            throw new Error(msg);
        },

        async onApprove(data) {
            try {
                const response = await fetch('/checkout/paypal/orders/' + data.orderID + '/capture', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'same-origin'
                });
                const result = await response.json();

                if (result.redirectUrl) {
                    window.location.href = result.redirectUrl;
                    return;
                }
                if (result.error) {
                    showMessage(result.error, true);
                    return;
                }
                showMessage('Unexpected PayPal response.', true);
            } catch (err) {
                console.error(err);
                showMessage('Payment could not be completed. ' + err, true);
            }
        },

        onCancel() {
            showMessage('PayPal payment cancelled.', true);
        },

        onError(err) {
            console.error('PayPal button error', err);
            showMessage('PayPal error: ' + (err.message || err), true);
        }
    }).render('#paypal-button-container').catch(function (err) {
        console.error(err);
        showMessage('Could not render PayPal buttons: ' + (err.message || err), true);
    });
};
