parent.clearTimeout(parent.document.enableEditBtnTimeout)
parent.editBtn.disabled = true;
parent.setEnableEditBtnTimeout();

var reloadTime = 1000;

setTimeout(function(){
    solvingLabel.innerText += "."
},reloadTime * 1/4)

setTimeout(function(){
    solvingLabel.innerText += "."
},reloadTime * 2/4)

setTimeout(function(){
    solvingLabel.innerText += "."
},reloadTime * 3/4)

setTimeout(function(){
    location.reload();
},reloadTime)