package com.panxoloto.sharepoint.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.panxoloto.sharepoint.rest.helper.AuthTokenHelperOnline;
import com.panxoloto.sharepoint.rest.helper.HeadersHelper;
import com.panxoloto.sharepoint.rest.helper.Permission;

public class PLGSharepointClientOnline implements PLGSharepointClient {


	private static final Logger LOG = LoggerFactory.getLogger(com.panxoloto.sharepoint.rest.PLGSharepointClientOnline.class);
	private MultiValueMap<String, String> headers;
	private RestTemplate restTemplate;
	private String spSiteUrl;
	private AuthTokenHelperOnline tokenHelper;
	private HeadersHelper headerHelper;
	
	/**
	 * @param spSiteUr.- The sharepoint site URL like https://contoso.sharepoint.com/sites/contososite
	 */
	/**
	 * @param user - The user email to access sharepoint online site.
	 * @param passwd - the user password to access sharepoint online site.
	 * @param domain - the domain without protocol and no uri like contoso.sharepoint.com
	 * @param spSiteUrl - The sharepoint site URI like /sites/contososite
	 */
	public PLGSharepointClientOnline(String user, 
			String passwd, String domain, String spSiteUrl) {
		super();
		this.restTemplate = new RestTemplate();
		this.spSiteUrl = spSiteUrl;
		if (this.spSiteUrl.endsWith("/")) {
			LOG.debug("spSiteUri ends with /, removing character");
			this.spSiteUrl = this.spSiteUrl.substring(0, this.spSiteUrl.length() - 1);
		}
		if (!this.spSiteUrl.startsWith("/")) {
			LOG.debug("spSiteUri doesnt start with /, adding character");
			this.spSiteUrl = String.format("%s%s", "/", this.spSiteUrl);
		}
		this.tokenHelper = new AuthTokenHelperOnline(this.restTemplate, user, passwd, domain, spSiteUrl);
		try {
			LOG.debug("Wrapper auth initialization performed successfully. Now you can perform actions on the site.");
			this.tokenHelper.init();
			this.headerHelper = new HeadersHelper(this.tokenHelper);
		} catch (Exception e) {
			LOG.error("Initialization failed!! Please check the user, pass, domain and spSiteUri parameters you provided", e);
		}
	}

	/**
	 * @throws Exception
	 */
	@Override
	public void refreshToken() throws Exception {
		this.tokenHelper.init();
	}
	
	/**
	 * Method to get json string wich you can transform to a JSONObject and get data from it.
	 * 
	 * 
	 * @param data - Data to be sent as query (look at the rest api documentation on how to include search filters).
	 * @return.- String representing a json object if the auth is correct.
	 * @throws Exception
	 */
	@Override
	public JSONObject getAllLists(String data) throws Exception {
		LOG.debug("getAllLists {}", data);
	    headers = headerHelper.getGetHeaders(false);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/lists");
		LOG.debug(String.format("URI: %s", uri ));

	    RequestEntity<String> requestEntity = new RequestEntity<>(data, 
	        headers, HttpMethod.GET, 
	       	uri
	        );

	    ResponseEntity<String> responseEntity = 
	        restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

	    return new JSONObject(responseEntity.getBody());
	}
	
	/**
	 * Retrieves list info by list title.
	 * 
	 * @param title - Site list title to query info.
	 * @param jsonExtendedAttrs - form json body for advanced query (take a look at ms documentation about rest api).
	 * @return json string with list information that can be used to get a JSONObject to use this info.
	 * @throws Exception thrown when something went wrong.
	 */
	@Override
	public JSONObject getListByTitle(String title, String jsonExtendedAttrs) throws Exception {
		LOG.debug("getListByTitle {} jsonExtendedAttrs {}", new Object[] {title, jsonExtendedAttrs});
	    headers = headerHelper.getGetHeaders(false);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/lists/GetByTitle('" + title + "')");
		LOG.debug(String.format("URI: %s", uri ));

	    RequestEntity<String> requestEntity = new RequestEntity<>(jsonExtendedAttrs, 
	        headers, HttpMethod.GET, 
	        uri
	        );

	    ResponseEntity<String> responseEntity = 
	        restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

	    return new JSONObject(responseEntity.getBody());
	}

	/**
	 * Retrieves list info by list title.
	 * 
	 * @param title - Site list title to query info.
	 * @return json string with list information that can be used to get a JSONObject to use this info.
	 * @throws Exception thrown when something went wrong.
	 */
	@Override
	public JSONObject getListFields(String title) throws Exception {
		LOG.debug("getListByTitle {} ", new Object[] {title});
	    headers = headerHelper.getGetHeaders(false);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/lists/GetByTitle('" + title + "')/Fields");
		LOG.debug(String.format("URI: %s", uri ));

	    RequestEntity<String> requestEntity = new RequestEntity<>("{}", 
	        headers, HttpMethod.GET, 
	        uri
	        );

	    ResponseEntity<String> responseEntity = 
	        restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

	    return new JSONObject(responseEntity.getBody());
	}

	/**
	 * @param listTitle
	 * @param description
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject createList(String listTitle, String description) throws Exception {
		LOG.debug("createList siteUrl {} listTitle {} description {}", new Object[] {listTitle, description});
		JSONObject payload = new JSONObject();
		JSONObject meta = new JSONObject();
		meta.put("type", "SP.List");
		payload.put("__metadata", meta);
		payload.put("AllowContentTypes", true);
		payload.put("BaseTemplate", 100);
		payload.put("ContentTypesEnabled", true);
		payload.put("Description", description);
		payload.put("Title", listTitle);
		
		String payloadStr = payload.toString();
		headers = headerHelper.getPostHeaders(payloadStr);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/lists");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(payloadStr,
    			headers, HttpMethod.POST, 
    			uri
    			);
	    ResponseEntity<String> responseEntity =  restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    return new JSONObject(responseEntity.getBody());
	}

	/**
	 * @param listTitle
	 * @param newDescription
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject updateList(String listTitle, String newDescription) throws Exception {
		LOG.debug("createList siteUrl {} listTitle {} description {}", new Object[] {listTitle, newDescription});
		JSONObject payload = new JSONObject();
		JSONObject meta = new JSONObject();
		meta.put("type", "SP.List");
		payload.put("__metadata", meta);
		if (newDescription != null) {
			payload.put("Description", newDescription);
		}

		String payloadStr = payload.toString();
		headers = headerHelper.getUpdateHeaders(payloadStr);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/lists/GetByTitle('" + listTitle + "')");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(payloadStr,
    			headers, HttpMethod.POST, 
    			uri
    			);
	    ResponseEntity<String> responseEntity =  restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    return new JSONObject(responseEntity.getBody());
	}

	/**
	 * @param title
	 * @param jsonExtendedAttrs
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject getListItems(String title, String jsonExtendedAttrs, String filter) throws Exception {
		LOG.debug("getListByTitle {} jsonExtendedAttrs {}", new Object[] {title, jsonExtendedAttrs});
	    headers = headerHelper.getGetHeaders(true);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/lists/GetByTitle('" + title + "')/items", filter);
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(jsonExtendedAttrs,
	        headers, HttpMethod.GET, 
	        uri
	        );

	    ResponseEntity<String> responseEntity = 
	        restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

	    return new JSONObject(responseEntity.getBody());
	}

	/**
	 * @param folder folder server relative URL to check (/SITEURL/folder)
	 * @param jsonExtendedAttrs extended body for the query.
	 * @return json string representing folder info.
	 * @throws Exception thrown when something went wrong.
	 */
	@Override
	public JSONObject checkFolderExist(String folder, String jsonExtendedAttrs) throws Exception {
		LOG.debug("getFolderByRelativeUrl {} jsonExtendedAttrs {}", new Object[] {folder, jsonExtendedAttrs});
		headers = headerHelper.getGetHeaders(false);

		URI uri = this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')/Exists");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(jsonExtendedAttrs,
				headers, HttpMethod.GET,
				uri
		);

		ResponseEntity<String> responseEntity =
				restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

		return new JSONObject(responseEntity.getBody());
	}
	
	/**
	 * @param folder folder server relative URL to retrieve (/SITEURL/folder)
	 * @param jsonExtendedAttrs extended body for the query.
	 * @return json string representing folder info.
	 * @throws Exception thrown when something went wrong.
	 */
	@Override
	public JSONObject getFolderByRelativeUrl(String folder, String jsonExtendedAttrs) throws Exception {
		LOG.debug("getFolderByRelativeUrl {} jsonExtendedAttrs {}", new Object[] {folder, jsonExtendedAttrs});
	    headers = headerHelper.getGetHeaders(false);

		URI uri = this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')");
		LOG.debug(String.format("URI: %s", uri ));

	    RequestEntity<String> requestEntity = new RequestEntity<>(jsonExtendedAttrs, 
	        headers, HttpMethod.GET, 
	        uri
	        );

	    ResponseEntity<String> responseEntity = 
	        restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

	    return new JSONObject(responseEntity.getBody());
	}

	/**
	 * @param folder folder server relative URL to retrieve (/SITEURL/folder)
	 * @param jsonExtendedAttrs extended body for the query.
	 * @return json string representing list of folders.
	 * @throws Exception thrown when something went wrong.
	 */
	@Override
	public JSONObject getFolderFoldersByRelativeUrl(String folder, String jsonExtendedAttrs) throws Exception {
		LOG.debug("getFolderFoldersByRelativeUrl {} jsonExtendedAttrs {}", new Object[] {folder, jsonExtendedAttrs});
		headers = headerHelper.getGetHeaders(false);

		URI uri = this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')/Folders");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(jsonExtendedAttrs,
			  headers, HttpMethod.GET,
			  uri
		);

		ResponseEntity<String> responseEntity =
				restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

		return new JSONObject(responseEntity.getBody());
	}

	@Override
	public JSONObject getFolderFilesByRelativeUrl(String folderServerRelativeUrl) throws Exception {
		LOG.debug("getFolderFilesByRelativeUrl {} ", new Object[] {folderServerRelativeUrl});
		headers = headerHelper.getGetHeaders(false);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folderServerRelativeUrl + "')/Files");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("{}",
			  headers, HttpMethod.GET,
			  uri
		);

		ResponseEntity<String> responseEntity =
				restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

		return new JSONObject(responseEntity.getBody());
	}
	
	/**
	 * @param folder folder server relative URL to retrieve (/SITEURL/folder)
	 * @param jsonExtendedAttrs extended body for the query.
	 * @return json string representing list of files.
	 * @throws Exception thrown when something went wrong.
	 */
	@Override
	public JSONObject getFolderFilesByRelativeUrl(String folder, String jsonExtendedAttrs) throws Exception {
		LOG.debug("getFolderFilesByRelativeUrl {} jsonExtendedAttrs {}", new Object[] {folder, jsonExtendedAttrs});
		headers = headerHelper.getGetHeaders(false);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')/Files");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(jsonExtendedAttrs,
			  headers, HttpMethod.GET,
			  uri
		);

		ResponseEntity<String> responseEntity =
				restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

		return new JSONObject(responseEntity.getBody());
	}


	/**
	 * @param fileServerRelativeUrl
	 * @return
	 * @throws Exception
	 */
	@Override
	public Boolean deleteFile(String fileServerRelativeUrl) throws Exception {
		LOG.debug("Deleting file {} ", fileServerRelativeUrl);

	    headers = headerHelper.getDeleteHeaders();

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFileByServerRelativeUrl('" + spSiteUrl + "/" + fileServerRelativeUrl +"')");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("{}",
	        headers, HttpMethod.POST, 
	        uri
	        );

	    restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    return Boolean.TRUE;
	}


	/**
	 * @param fileServerRelativeUrl
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject getFileInfo(String fileServerRelativeUrl) throws Exception {
		LOG.debug("Getting file info {} ", fileServerRelativeUrl);

		headers = headerHelper.getGetHeaders(true);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFileByServerRelativeUrl('" + spSiteUrl + "/" + fileServerRelativeUrl +"')");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("",
			  headers, HttpMethod.GET,
			  uri
		);

		ResponseEntity<String> responseEntity = restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);
		return new JSONObject(responseEntity.getBody());
	}

	/**
	 * @param fileServerRelativeUrl
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject getFileSpecificInfo(String fileServerRelativeUrl, String info) throws Exception {
		LOG.debug("Getting file info {} ", fileServerRelativeUrl);

		headers = headerHelper.getGetHeaders(true);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFileByServerRelativeUrl('" + spSiteUrl + "/" + fileServerRelativeUrl +"')/" + info);
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("",
				headers, HttpMethod.GET,
				uri
		);

		ResponseEntity<String> responseEntity = restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);
		return new JSONObject(responseEntity.getBody());
	}

	/**
	 * @param fileServerRelativeUrl
	 * @return
	 * @throws Exception
	 */
	@Override
	public Resource downloadFile(String fileServerRelativeUrl) throws Exception {
		LOG.debug("Downloading file {} ", fileServerRelativeUrl);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFileByServerRelativeUrl('" + spSiteUrl + "/" + fileServerRelativeUrl +"')/$value");
		LOG.debug(String.format("URI: %s", uri ));

		headers = headerHelper.getGetHeaders(true);
	    
	    RequestEntity<String> requestEntity = new RequestEntity<>("", 
	        headers, HttpMethod.GET, 
	        uri
	        );

	    ResponseEntity<Resource> response = restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, Resource.class);
	    return response.getBody();
	}

	/**
	 * @param fileServerRelativeUrl
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	@Override
	public Resource downloadFile(String fileServerRelativeUrl, String fileName) throws Exception {
		LOG.debug("Downloading file {} ", fileServerRelativeUrl);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + fileServerRelativeUrl +"')/Files('" + fileName + "')/$value");
		LOG.debug(String.format("URI: %s", uri ));

		headers = headerHelper.getGetHeaders(true);

		RequestEntity<String> requestEntity = new RequestEntity<>("",
				headers, HttpMethod.GET,
				uri
		);

		ResponseEntity<Resource> response = restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, Resource.class);
		return response.getBody();
	}

	/**
	 * @param folder
	 * @param resource
	 * @param jsonMetadata
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject uploadFile(String folder, Resource resource, JSONObject jsonMetadata) throws Exception {
		LOG.debug("Uploading file {} to folder {}", resource.getFilename(), folder);

		JSONObject submeta = new JSONObject();
		if (jsonMetadata.has("type")) {
			submeta.put("type", jsonMetadata.get("type"));
		} else {
			submeta.put("type", "SP.ListItem");
		}
		jsonMetadata.put("__metadata", submeta);
		
	    headers = headerHelper.getPostHeaders("");


		URI uri =  this.tokenHelper.getSharepointSiteUrl(
				"/_api/web/GetFolderByServerRelativeUrl('" + folder +"')/Files/add(url='"
						+ resource.getFilename() + "',overwrite=true)"
		);
		LOG.debug(String.format("URI: %s", uri ));
		LOG.debug(String.format("Resource: %s -> %s", resource.getFilename(), resource.toString() ));
		LOG.debug(String.format("Headers"));
		for (String h : headers.keySet())
			LOG.debug(String.format("%s -> %s",h, headers.get(h)));

		RequestEntity<Resource> requestEntity = new RequestEntity<>(resource,
	        headers, HttpMethod.POST,
	        uri
	        );

	    ResponseEntity<String> responseEntity = 
	        restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);

	    String fileInfoStr = responseEntity.getBody();
	    
	    LOG.debug("Retrieved response from server with json");
	    
	    JSONObject jsonFileInfo = new JSONObject(fileInfoStr);
	    String serverRelFileUrl = jsonFileInfo.getJSONObject("d").getString("ServerRelativeUrl");

	    LOG.debug("File uploaded to URI", serverRelFileUrl);
	    String metadata = jsonMetadata.toString();
	    headers = headerHelper.getUpdateHeaders(metadata);

		URI uri1 =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFileByServerRelativeUrl('" + serverRelFileUrl + "')/listitemallfields");
		LOG.debug(String.format("URI: %s", uri1 ));

		LOG.debug("Updating file adding metadata {}", jsonMetadata);

	    RequestEntity<String> requestEntity1 = new RequestEntity<>(metadata, 
	        headers, HttpMethod.POST,
	        uri1
	        );
	    ResponseEntity<String> responseEntity1 = 
		        restTemplate.exchange(uri1.toString(), HttpMethod.POST, requestEntity1, String.class);
	    LOG.debug("Updated file metadata Status {}", responseEntity1.getStatusCode());
	    return jsonFileInfo;
	}


	public JSONObject uploadFileDev(String folder, Resource resource, JSONObject jsonMetadata) throws Exception {
		LOG.debug("Uploading file {} to folder {}", resource.getFilename(), folder);
		JSONObject subMeta = new JSONObject();
		if (jsonMetadata.has("type")) {
			subMeta.put("type", jsonMetadata.get("type"));
		} else {
			subMeta.put("type", "SP.ListItem");
		}
		jsonMetadata.put("__metadata", subMeta);

		headers = headerHelper.getPostHeaders("");

		URI uri =  this.tokenHelper.getSharepointSiteUrl(
				"/_api/Web/getFolderByServerRelativePath(DecodedUrl='" + folder + "')/Files/add(overwrite=true," +
						"url='"	+ resource.getFilename() + "')"
		);

		LOG.debug(String.format("URI: %s", uri ));
		LOG.debug(String.format("Resource: %s -> %s", resource.getFilename(), resource.toString() ));
		LOG.debug(String.format("Headers"));

		for (String h : headers.keySet())
			LOG.debug(String.format("%s -> %s",h, headers.get(h)));

		RequestEntity<Resource> requestEntity = new RequestEntity<>(resource,
				headers, HttpMethod.POST,
				uri
		);

		ResponseEntity<String> responseEntity =
				restTemplate.exchange(requestEntity, String.class);

		String fileInfoStr = responseEntity.getBody();

		LOG.debug("Retrieved response from server with json");

		JSONObject jsonFileInfo = new JSONObject(fileInfoStr);
		String serverRelFileUrl = jsonFileInfo.getJSONObject("d").getString("ServerRelativeUrl");

		LOG.debug("File uploaded to URI {}", serverRelFileUrl);
		String metadata = jsonMetadata.toString();
		headers = headerHelper.getUpdateHeaders(metadata);

		LOG.debug("Updating file adding metadata {}", jsonMetadata);

		RequestEntity<String> requestEntity1 = new RequestEntity<>(metadata,
				headers, HttpMethod.POST,
				this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFileByServerRelativeUrl('" + serverRelFileUrl + "')/listitemallfields")
		);
		ResponseEntity<String> responseEntity1 =
				restTemplate.exchange(requestEntity1, String.class);
		LOG.debug("Updated file metadata Status {}", responseEntity1.getStatusCode());
		return jsonFileInfo;
	}

	/**
	 * @param folder
	 * @param resource
	 * @param jsonMetadata
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject uploadFile(String folder, Resource resource, String fileName, JSONObject jsonMetadata) throws Exception {
		LOG.debug("Uploading file {} to folder {}", fileName, folder);
		JSONObject submeta = new JSONObject();
		submeta.put("type", "SP.ListItem");
		jsonMetadata.put("__metadata", submeta);
		
	    headers = headerHelper.getPostHeaders("");
	    headers.remove("Content-Length");

		URI uri =  this.tokenHelper.getSharepointSiteUrl(
				"/_api/web/GetFolderByServerRelativeUrl('" + folder +"')/Files/add(url='"
						+ fileName + "',overwrite=true)"
		);

		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<Resource> requestEntity = new RequestEntity<>(resource,
	        headers, HttpMethod.POST, 
	        uri
	        );

	    ResponseEntity<String> responseEntity = 
	        restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);

	    String fileInfoStr = responseEntity.getBody();
	    
	    LOG.debug("Retrieved response from server with json");
	    
	    JSONObject jsonFileInfo = new JSONObject(fileInfoStr);
	    String serverRelFileUrl = jsonFileInfo.getJSONObject("d").getString("ServerRelativeUrl");

	    LOG.debug("File uploaded to URI", serverRelFileUrl);
	    String metadata = jsonMetadata.toString();
	    headers = headerHelper.getUpdateHeaders(metadata);

	    LOG.debug("Updating file adding metadata {}", jsonMetadata);

		URI uri1 =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFileByServerRelativeUrl('" + serverRelFileUrl + "')/listitemallfields");
		LOG.debug(String.format("URI: %s", uri1 ));

		RequestEntity<String> requestEntity1 = new RequestEntity<>(metadata,
	        headers, HttpMethod.POST, 
	        uri1
	        );
	    ResponseEntity<String> responseEntity1 =
		        restTemplate.exchange(uri1.toString(), HttpMethod.POST, requestEntity1, String.class);
	    LOG.debug("Updated file metadata Status {}", responseEntity1.getStatusCode());
	    return jsonFileInfo;
	}
	
	/**
	 * @param fileServerRelatUrl
	 * @param jsonMetadata
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject updateFileMetadata(String fileServerRelatUrl, JSONObject jsonMetadata) throws Exception {
		JSONObject meta = new JSONObject();
		if (jsonMetadata.has("type")) {
			meta.put("type", jsonMetadata.get("type"));
		} else {
			meta.put("type", "SP.File");
		}
		jsonMetadata.put("__metadata", meta);
	    LOG.debug("File uploaded to URI", fileServerRelatUrl);
	    String metadata = jsonMetadata.toString();
	    headers = headerHelper.getUpdateHeaders(metadata);
	    LOG.debug("Updating file adding metadata {}", jsonMetadata);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFileByServerRelativeUrl('" + fileServerRelatUrl + "')/listitemallfields");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(metadata,
	        headers, HttpMethod.POST, 
	        uri
	        );
	    ResponseEntity<String> responseEntity =
		        restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    LOG.debug("Updated file metadata Status {}", responseEntity.getStatusCode());
	    return new JSONObject(responseEntity);
	}
	
	/**
	 * @param folderServerRelatUrl
	 * @param jsonMetadata
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject updateFolderMetadata(String folderServerRelatUrl, JSONObject jsonMetadata) throws Exception {
		JSONObject meta = new JSONObject();
		if (jsonMetadata.has("type")) {
			meta.put("type", jsonMetadata.get("type"));
		} else {
			meta.put("type", "SP.Folder");
		}
		jsonMetadata.put("__metadata", meta);
	    LOG.debug("File uploaded to URI", folderServerRelatUrl);
	    String metadata = jsonMetadata.toString();
	    headers = headerHelper.getUpdateHeaders(metadata);
	    LOG.debug("Updating file adding metadata {}", jsonMetadata);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folderServerRelatUrl + "')/listitemallfields");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(metadata,
	        headers, HttpMethod.POST, 
	        uri
	        );
	    ResponseEntity<String> responseEntity =
		        restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    LOG.debug("Updated file metadata Status {}", responseEntity.getStatusCode());
	    return new JSONObject(responseEntity);
	}
	
	/**
	 * @param folder
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject breakRoleInheritance(String folder) throws Exception {
		LOG.debug("Breaking role inheritance on folder {}", folder);
	    headers = headerHelper.getPostHeaders("");

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')/ListItemAllFields/breakroleinheritance(copyRoleAssignments=false,clearSubscopes=true)");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("",
	        headers, HttpMethod.POST, 
	        uri
        );

	    ResponseEntity<String> responseEntity =  restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    return new JSONObject(responseEntity.getBody());
	}

	/**
	 * @param baseFolderRemoteRelativeUrl
	 * @param folder
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject createFolder(String baseFolderRemoteRelativeUrl, String folder, JSONObject payload) throws Exception {
		LOG.debug("createFolder baseFolderRemoteRelativeUrl {} folder {}", new Object[] {baseFolderRemoteRelativeUrl, folder});

		if (payload == null) {
			payload = new JSONObject();
		}
		JSONObject meta = new JSONObject();
		if (payload.has("type")) {
			meta.put("type", payload.get("type"));
		} else {
			meta.put("type", "SP.Folder");
		}
		payload.put("__metadata", meta);
		payload.put("ServerRelativeUrl", folder);
		String payloadStr = payload.toString();
		headers = headerHelper.getPostHeaders(payloadStr);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" +  baseFolderRemoteRelativeUrl + "')/folders");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>(payloadStr,
    			headers, HttpMethod.POST, 
    			uri
    			);
	    ResponseEntity<String> responseEntity =  restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    return new JSONObject(responseEntity.getBody());
	}
	
	/**
	 * @param sourceRelativeServerUrl
	 * @param destinyRelativeServerUrl
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject moveFolder(String sourceRelativeServerUrl, String destinyRelativeServerUrl) throws Exception {
		LOG.debug("createFolder sourceRelativeServerUrl {} destinyRelativeServerUrl {}", new Object[] {sourceRelativeServerUrl, destinyRelativeServerUrl});
		headers = headerHelper.getPostHeaders("");

		URI uri =  this.tokenHelper.getSharepointSiteUrl(
				"/_api/web/GetFolderByServerRelativeUrl('" + sourceRelativeServerUrl
						+ "')/moveto(newUrl='" + destinyRelativeServerUrl + "',flags=1)"
		);
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("",
    			headers, HttpMethod.POST, 
    			uri
    			);
	    ResponseEntity<String> responseEntity =  restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    return new JSONObject(responseEntity.getBody());
	}
	
	/**
	 * @param sourceRelativeServerUrl
	 * @param destinyRelativeServerUrl
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject moveFile(String sourceRelativeServerUrl, String destinyRelativeServerUrl) throws Exception {
		LOG.debug("createFolder sourceRelativeServerUrl {} destinyRelativeServerUrl {}", new Object[] {sourceRelativeServerUrl, destinyRelativeServerUrl});
		headers = headerHelper.getPostHeaders("");

		URI uri =  this.tokenHelper.getSharepointSiteUrl(
				"/_api/web/GetFileByServerRelativeUrl('" + spSiteUrl + "/" + sourceRelativeServerUrl
						+ "')/moveto(newUrl='" + spSiteUrl + "/" + destinyRelativeServerUrl + "',flags=1)"
		);
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("",
    			headers, HttpMethod.POST, 
    			uri
    			);


	    ResponseEntity<String> responseEntity =  restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    return new JSONObject(responseEntity.getBody());
	}
	
	/**
	 * @param folderRemoteRelativeUrl
	 * @return
	 * @throws Exception
	 */
	@Override
	public Boolean removeFolder(String folderRemoteRelativeUrl) throws Exception {
		LOG.debug("Deleting folder {}", folderRemoteRelativeUrl);
		headers = headerHelper.getDeleteHeaders();

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folderRemoteRelativeUrl + "')");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("",
    			headers, HttpMethod.POST, 
    			uri
    			);
	    restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    return Boolean.TRUE;
	}

	/**
	 * @param folder
	 * @param users
	 * @param permission
	 * @return
	 * @throws Exception
	 */
	@Override
	public Boolean grantPermissionToUsers(String folder, List<String> users, Permission permission) throws Exception {
		LOG.debug("Granting {} permission to users {} in folder {}", new Object[] {permission, users, folder});

	    headers = headerHelper.getGetHeaders(false);

		List<Integer> userIds = new ArrayList<>();
	    for (String user : users) {

			URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/SiteUsers/getByEmail('" + user+ "')");
			LOG.debug(String.format("URI: %s", uri ));

	    	RequestEntity<String> requestEntity = new RequestEntity<>("{}", 
	    			headers, HttpMethod.GET, 
	    			uri
	    			);
	    	ResponseEntity<String> responseEntity =  restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);
	    	JSONObject objJson = new JSONObject(responseEntity.getBody());
	    	LOG.debug("json object retrieved for user {}", user);
	    	Integer userId = (Integer) objJson.getJSONObject("d").get("Id");
	    	userIds.add(userId);
	    }
	    
	    headers = headerHelper.getPostHeaders("{}");

	    for (Integer userId : userIds) {

			URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')/ListItemAllFields/roleAssignments/addroleassignment(principalid=" + userId +",roleDefId=" + permission +")");
			LOG.debug(String.format("URI: %s", uri));

	    	RequestEntity<String> requestEntity = new RequestEntity<>("{}",
	    			headers, HttpMethod.POST, 
	    			uri
    			);
	    	
	    	restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    }
	    return Boolean.TRUE;
	}
	
	/**
	 * @param folder
	 * @return
	 * @throws Exception
	 */
	@Override
	public JSONObject getFolderPermissions(String folder) throws Exception {
		headers = headerHelper.getGetHeaders(false);

		URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')/ListItemAllFields/roleAssignments");
		LOG.debug(String.format("URI: %s", uri ));

		RequestEntity<String> requestEntity = new RequestEntity<>("{}",
	    		headers, HttpMethod.GET, 
				uri
	    		);
	    
	    ResponseEntity<String> response = restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);

	    return new JSONObject(response.getBody());
	}
	
	/**
	 * @param folder
	 * @param permission
	 * @return
	 * @throws Exception
	 */
	@Override
	public Boolean removePermissionToFolder(String folder, Permission permission) throws Exception {
	    List<Integer> userIds = new ArrayList<>();
	    JSONObject permissions = getFolderPermissions(folder);
	    JSONArray results = permissions.getJSONObject("d").getJSONArray("results");
	    for (int i = 0 ; i < results.length() ; i++) {
    		JSONObject jObj = results.getJSONObject(i);
    		Integer principalId = jObj.getInt("PrincipalId");
    		if (principalId != null && !userIds.contains(principalId)) {
    			userIds.add(principalId);
    		}
    		LOG.debug("JSON payload retrieved from server for user {}", "");
	    }
	    
	    headers = headerHelper.getDeleteHeaders();
	    for (Integer userId : userIds) {

			URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')/ListItemAllFields/roleAssignments/getbyprincipalid(" + userId  +")");
			LOG.debug(String.format("URI: %s", uri ));

			RequestEntity<String> requestEntity = new RequestEntity<>("{}",
	    			headers, HttpMethod.POST, 
	    			uri
			);
	    	
	    	restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity, String.class);
	    }
	    return Boolean.TRUE;
	}
	
	
	/**
	 * @param folder
	 * @param users
	 * @param permission
	 * @return
	 * @throws Exception
	 */
	@Override
	public Boolean removePermissionToUsers(String folder, List<String> users, Permission permission) throws Exception {
		LOG.debug("Revoking {} permission to users {} in folder {}", new Object[] {permission, users, folder});
		
	    headers = headerHelper.getGetHeaders(false);

	    List<Integer> userIds = new ArrayList<>();
	    for (String user : users) {

			URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/SiteUsers/getByEmail('" + user+ "')");
			LOG.debug(String.format("URI: %s", uri ));

			RequestEntity<String> requestEntity = new RequestEntity<>("{}",
	    			headers, HttpMethod.GET, 
	    			uri
			);
	    	ResponseEntity<String> responseEntity =  restTemplate.exchange(uri.toString(), HttpMethod.GET, requestEntity, String.class);
	    	LOG.debug("JSON payload retrieved from server for user {}", user);
	    	JSONObject objJson = new JSONObject(responseEntity.getBody());
	    	Integer userId = (Integer) objJson.getJSONObject("d").get("Id");
	    	userIds.add(userId);
	    }
	    
	    headers = headerHelper.getDeleteHeaders();
	    for (Integer userId : userIds) {

			URI uri =  this.tokenHelper.getSharepointSiteUrl("/_api/web/GetFolderByServerRelativeUrl('" + folder + "')/ListItemAllFields/roleAssignments/getbyprincipalid(" + userId  +")");
			LOG.debug(String.format("URI: %s", uri ));

			RequestEntity<String> requestEntity1 = new RequestEntity<>("{}",
	    			headers, HttpMethod.POST, 
	    			uri
			);
	    	
	    	restTemplate.exchange(uri.toString(), HttpMethod.POST, requestEntity1, String.class);
	    }
	    return Boolean.TRUE;
	}

}
