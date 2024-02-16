$(document).ready(()=>{
    let root_element = document.querySelector("#root");
    let reload_element = document.createElement("div");
    reload_element.style.zIndex="9";
    reload_element.style.position = "fixed";
    reload_element.style.bottom = '30px';
    reload_element.style.left = '30px';
    reload_element.style.width = "50px";
    reload_element.style.height = "50px";
    reload_element.style.borderRadius = '50%';
    reload_element.style.padding = '15px';
    reload_element.style.paddingLeft = '13px';
    reload_element.style.backgroundColor = "var(--hope-colors-primary11)";
    reload_element.innerHTML = '<strong class="character" style="font-size: 25px;line-height: 0.5;">⟳</strong>';
    reload_element.addEventListener("click", ()=>{
        appHandler.reloadPage();
    });
    reload_element.addEventListener("dblclick", ()=>{
        appHandler.reloadPage();
    });
    root_element.appendChild(reload_element);
})

setTimeout(()=>{
    $(".footer")[0].innerHTML += `<p style="margin:12px;"><a onclick="appHandler.inputNewURL()">修改Alist URL</a></p>`;
},500)