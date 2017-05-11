var r = new Resumable({
  target:'/api/photo/redeem-upload-token',
  query:{upload_token:'my_token'}
});

if(!r.support) location.href = '/some-old-crappy-uploader';

r.assignBrowse(document.getElementById('browse'));
r.assignDrop(document.getElementById(''));

