(function(){
  if(window.fm&&window.fongmi){window.dispatchEvent(new CustomEvent('fmsdk'));return;}
  if(document&&document.documentElement)document.documentElement.classList.add('fm-native');
  window.fongmiClient={mode:'__FM_MODE__',isLeanback:__FM_LEANBACK__};
  var callbacks={};
  var seq=0;
  function invoke(method,payload){
    return new Promise(function(resolve,reject){
      var id='fm_'+Date.now()+'_'+(++seq);
      callbacks[id]={resolve:resolve,reject:reject};
      fongmiBridge.invoke(id,method,JSON.stringify(payload||{}));
    });
  }
  function hydrate(data){
    if(!data||!data.__fmResultId)return data;
    var resultId=data.__fmResultId;
    var length=fongmiBridge.resultLength(resultId);
    var text='';
    for(var start=0;start<length;start+=60000)text+=fongmiBridge.resultChunk(resultId,start);
    fongmiBridge.clearResult(resultId);
    return JSON.parse(text);
  }
  window.fongmiNative={
    resolve:function(id,data){ if(callbacks[id]){ callbacks[id].resolve(hydrate(data)); delete callbacks[id]; } },
    reject:function(id,error){ if(callbacks[id]){ callbacks[id].reject(new Error(error||'')); delete callbacks[id]; } }
  };
  if(!window.__fmUrlHook&&window.history){
    window.__fmUrlHook=true;
    var emit=function(){window.dispatchEvent(new CustomEvent('fmurlchange',{detail:{url:location.href}}));};
    var rawPush=history.pushState;
    var rawReplace=history.replaceState;
    history.pushState=function(){var r=rawPush.apply(this,arguments);emit();return r;};
    history.replaceState=function(){var r=rawReplace.apply(this,arguments);emit();return r;};
    window.addEventListener('popstate',emit);
  }
  var player={
    playUrl:function(url,title,options){return invoke('player.playUrl',Object.assign({},options||{},{url:url,title:title}));},
    playVod:function(siteKey,vodId,title,pic,options){return invoke('player.playVod',Object.assign({},options||{},{siteKey:siteKey,vodId:vodId,title:title,pic:pic}));},
    preloadArtwork:function(pic,wallPic){return invoke('player.preloadArtwork',{pic:pic,wallPic:wallPic});},
    status:function(){return invoke('player.status',{});}
  };
  var net={
    request:function(url,options){return invoke('net.request',Object.assign({},options||{},{url:url}));}
  };
  var cache={
    get:function(key,rule){return invoke('cache.get',{key:key,rule:rule});},
    set:function(key,value,rule){return invoke('cache.set',{key:key,value:value,rule:rule});},
    del:function(key,rule){return invoke('cache.del',{key:key,rule:rule});}
  };
  var pan={
    check:function(items){return invoke('pan.check',{items:items});},
    play:function(payload){return invoke('pan.play',payload||{});}
  };
  var ui={
    setToolbar:function(visible){return invoke('ui.setToolbar',{visible:visible!==false});},
    getViewport:function(){return invoke('ui.getViewport',{});}
  };
  var ext={
    info:function(){return invoke('ext.info',{});},
    toast:function(message){return invoke('ext.toast',{message:message});}
  };
  window.fongmi={invoke:invoke,player:player,net:net,cache:cache,
    app:{
      search:function(keyword,options){return invoke('app.search',Object.assign({},options||{},{keyword:keyword}));},
      openVod:function(){return invoke('app.openVod',{});},
      openLive:function(){return invoke('app.openLive',{});},
      openKeep:function(){return invoke('app.openKeep',{});},
      openSetting:function(){return invoke('app.openSetting',{});},
      history:function(){return invoke('app.history',{});}
    },
    pan:pan,
    ext:ext,
    device:{info:function(){return invoke('device.info',{});}},
    site:{info:function(){return invoke('site.info',{});}},
    config:{info:function(){return invoke('config.info',{});}},
    ui:ui,
    navigation:{
      back:function(){return invoke('navigation.back',{});},
      reload:function(){return invoke('navigation.reload',{});}
    }
  };
  window.fm={
    req:net.request,
    play:player.playUrl,
    vod:player.playVod,
    preloadArtwork:player.preloadArtwork,
    stat:player.status,
    search:window.fongmi.app.search,
    openVod:window.fongmi.app.openVod,
    openLive:window.fongmi.app.openLive,
    openKeep:window.fongmi.app.openKeep,
    openSetting:window.fongmi.app.openSetting,
    history:window.fongmi.app.history,
    pan:pan,
    check:pan.check,
    cache:cache,
    ext:ext,
    device:window.fongmi.device.info,
    site:window.fongmi.site.info,
    config:window.fongmi.config.info,
    ui:ui,
    back:window.fongmi.navigation.back,
    reload:window.fongmi.navigation.reload
  };
  window.dispatchEvent(new CustomEvent('fmsdk'));
})();
