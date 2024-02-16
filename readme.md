# Alist Explorer

Before use, add this html headers with app necessary working dependencies and initialize call in AList Manage > Settings > Global > Customize head

```html
<script src="https://lf3-cdn-tos.bytecdntp.com/cdn/expire-1-M/jquery/2.1.3/jquery.min.js"></script>
<script src="https://unpkg.com/minirefresh@2.0.2/dist/minirefresh.min.js"></script>
<link rel="stylesheet" href="https://unpkg.com/minirefresh@2.0.2/dist/minirefresh.min.css">
<script>
try{
appHandler.init();
}catch(err){}
</script>
```