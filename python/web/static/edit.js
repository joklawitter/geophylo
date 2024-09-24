const queryString = window.location.search;
const urlParams = new URLSearchParams(queryString);

document.body.onload = function(){
    requestNewSvg()
}

editForm.onchange = requestNewSvg
editForm.onreset = function(){
    setTimeout(function(){
        requestNewSvg()
    }, 50);
}

dangerForm.onchange = function(){
    showDangerForm(true)
}
dangerForm.onreset = function(){
    setTimeout(function(){
        showDangerForm(false)
    }, 50);
}

function giveNewSvgLink(){
    newSrcString = window.location.origin
    newSrcString += "/draw?id=" + urlParams.get('id');
    newSrcString += "&lh=" + leafHeightRange.value;
    newSrcString += "&ih=" + internalHeightRange.value;
    newSrcString += "&el=" + extraLeftRange.value;
    newSrcString += "&er=" + extraRightRange.value;
    newSrcString += "&eb=" + extraBotRange.value;
    newSrcString += "&bm=" + backgroundModeSelect.value;
    newSrcString += "&blm=" + branchLengthModeSelect.value;

    leafHeightRange.disabled = branchLengthModeSelect.value == "custom"

    return newSrcString;
}

function showDangerForm(pValue){
    if(pValue){
        editBtnDiv.style.display = "none";
        dangerBtnDiv.style.display = "inline";
        reOptimizeNoticeDiv.style.display = "inline";
    }else{
        editBtnDiv.style.display = "inline";
        dangerBtnDiv.style.display = "none";
        reOptimizeNoticeDiv.style.display = "none";
    }
}

function requestNewSvg(){
    newSvgLink = giveNewSvgLink();

    outSvgIframe.src = newSvgLink;
    downloadBtn.href = newSvgLink;
}
