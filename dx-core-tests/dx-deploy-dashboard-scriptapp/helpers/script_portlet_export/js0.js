function showIframe(iframeId) {
  // Hide all iframes
  var iframes = document.getElementById('iframe-container').getElementsByTagName('iframe');
  for (var i = 0; i < iframes.length; i++) {
    iframes[i].style.display = 'none';
  }

  // Show the selected iframe
  var iframe = document.getElementById(iframeId);
  iframe.src = iframe.src;
  iframe.style.display = 'block';
}

window.onload = function() {
        var iframe = document.getElementById("acceptance-tests-iframe");
        iframe.style.display = 'block';
}