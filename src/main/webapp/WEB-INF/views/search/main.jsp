<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<html>
<head>
	<title>图像搜索管理</title>
</head>

<body>
	<form id="inputForm" action="s" method="post" class="form-horizontal" enctype="multipart/form-data">

		<fieldset>
			<legend><small>图像搜索管理</small></legend>
			<div class="form-actions">
				<input id="index-btn" class="btn" type="button" value="建立图像索引"/>
			</div>
			<div class="control-group">
				图像文件：<input type="file" class="span2" name="file" />
				关键字：<input type="text" class="span2" name="title" />
				位置：<input type="text" class="span2" name="location" />
				经度：<input type="text" class="span1" name="lng" />
				纬度：<input type="text" class="span1" name="lat" />
				<input id="search-btn" class="btn" type="submit" value="搜索图像"/>
			</div>
			<c:if test="${fn:length(result) > 0 }">
			<table id="contentTable" class="table table-striped table-bordered table-condensed">
				<thead><tr><th>图像</th><th>标题</th><th>标签<th>位置</th></tr></thead>
				<tbody>
				<c:forEach items="${result}" var="r">
					<tr>
						<td></td>
						<td>${r.title}</td>
						<td>${r.tags}</td>
						<td>${r.location}</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
			</c:if>
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
