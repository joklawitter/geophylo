const queryString = window.location.search;
const urlParams = new URLSearchParams(queryString);

function setCookie(cname, cvalue, exdays) {
    const d = new Date();
    d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
    let expires = "expires=" + d.toUTCString();
    document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

function getCookie(cname) {
    let name = cname + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    for (let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

document.body.onload = function(){
    solutionsInCookies = getCookie("solutions")
    if(solutionsInCookies == ""){
        solutionsInCookiesJSON = []
    }else{
        solutionsInCookiesJSON = JSON.parse(solutionsInCookies)
    }
    console.log(solutionsInCookiesJSON)
    if(solutionsInCookiesJSON.indexOf(urlParams.get('id')) == -1){
        solutionsInCookiesJSON.push(urlParams.get('id'))
        setCookie("solutions",JSON.stringify(solutionsInCookiesJSON),365)
    }

    checkForSolution()
    displayPreview()
}

editBtn.onclick = function(){
    newSrcString = window.location.origin
    newSrcString += "/edit?id=" + urlParams.get('id');

    window.location = newSrcString;
}

function checkForSolution(){
    newSrcString = window.location.origin
    newSrcString += "/loading?id=" + urlParams.get('id');

    loadingIframe.src = newSrcString;
}

function displayPreview(){
    newSrcString = window.location.origin
    newSrcString += "/preview?id=" + urlParams.get('id');

    previewIframe.src = newSrcString;
}

function setEnableEditBtnTimeout(){
    document.enableEditBtnTimeout = setTimeout(function(){
        editBtn.disabled = false;
    },1500)
}