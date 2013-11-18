<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<html>
<head>
	<title>图像搜索管理</title>
</head>

<body>
	<form id="inputForm" action="${ctx}" method="post" class="form-horizontal">

		<fieldset>
			<legend><small>图像搜索管理</small></legend>
			<div class="control-group">
				当前已索引图像数量: 123
			</div>
			<div class="form-actions">
				<input id="search-btn" class="btn" type="button" value="搜索图像"/>
				<input id="index-btn" class="btn" type="button" value="建立图像索引"/>
			</div>
		</fieldset>
	</form>
	<script>
		$(document).ready(function() {
			$('#index-btn').click(function () {
				$('#inputForm').attr('action', 'index');
				$('#inputForm').submit();
			});
		});
	</script>
</body>
</html>
