const queryString = window.location.search;
const urlParams = new URLSearchParams(queryString);

document.body.onload = checkForErros;
inputForm.onchange = checkForErros;

function checkForErros(event){
    inputForm.target = "previewIframe";
    inputForm.action = window.location.origin + "/checkError"
    inputForm.submit();
    inputForm.target = "";
    inputForm.action = "";
}
onkeydown="disableOptimizeBtn()"
function showPreview(){
    inputForm.target = "previewIframe";
    inputForm.action = window.location.origin + "/preview"
    inputForm.submit();
    inputForm.target = "";
    inputForm.action = "";
}

function disableOptimizeBtn(){
    optimizeBtn.disabled = true;
}